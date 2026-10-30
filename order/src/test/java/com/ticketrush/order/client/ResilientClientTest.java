package com.ticketrush.order.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * Retry, circuit breaker and bulkhead around a real HTTP client, against WireMock. Same policy as
 * application.yml (retry I/O errors and 5xx, never 4xx), with smaller numbers so the test is fast.
 */
class ResilientClientTest {

    private static final String RESERVE = "/api/internal/reservations";

    private static final String RESERVATION = "{\"orderId\":1,\"expiresAt\":\"2030-01-01T00:00:00Z\",\"lines\":[]}";

    private static final InventoryClient.ReserveRequest REQUEST = new InventoryClient.ReserveRequest(
        1L,
        List.of(new InventoryClient.ReserveRequest.Item(7L, 1))
    );

    private WireMockServer inventory;

    private CircuitBreaker circuitBreaker;

    // A server per test: delayed responses of one test must not land in the next test's request journal
    @BeforeEach
    void startInventory() {
        inventory = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        inventory.start();
    }

    @AfterEach
    void stopInventory() {
        inventory.stop();
    }

    @Test
    void serverErrorsAreRetriedUntilTheCallSucceeds() {
        inventory.stubFor(post(RESERVE).inScenario("flaky").whenScenarioStateIs(Scenario.STARTED).willReturn(serviceUnavailable()).willSetStateTo("2"));
        inventory.stubFor(post(RESERVE).inScenario("flaky").whenScenarioStateIs("2").willReturn(serverError()).willSetStateTo("3"));
        inventory.stubFor(post(RESERVE).inScenario("flaky").whenScenarioStateIs("3").willReturn(okJson(RESERVATION)));

        assertThat(client(25).reserve(REQUEST).orderId()).isEqualTo(1L);
        inventory.verify(3, postRequestedFor(urlEqualTo(RESERVE)));
    }

    @Test
    void businessConflictsAreNeitherRetriedNorCountedAsFailures() {
        inventory.stubFor(post(RESERVE).willReturn(aResponse().withStatus(409)));
        InventoryClient client = client(25);

        assertThatThrownBy(() -> client.reserve(REQUEST)).isInstanceOf(HttpClientErrorException.Conflict.class);
        inventory.verify(1, postRequestedFor(urlEqualTo(RESERVE)));
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isZero();
    }

    @Test
    void slowAnswersTimeOutAndAreRetried() {
        inventory.stubFor(post(RESERVE).willReturn(okJson(RESERVATION).withFixedDelay(1_000)));
        InventoryClient client = client(25);

        long start = System.nanoTime();
        assertThatThrownBy(() -> client.reserve(REQUEST)).isInstanceOf(RestClientException.class);

        inventory.verify(3, postRequestedFor(urlEqualTo(RESERVE)));
        assertThat(Duration.ofNanos(System.nanoTime() - start)).as("3 attempts x 300 ms timeout").isLessThan(Duration.ofSeconds(3));
    }

    @Test
    void openCircuitFailsFastWithoutCallingTheDependency() {
        inventory.stubFor(post(RESERVE).willReturn(serverError()));
        InventoryClient client = client(25);

        // 1st call: three attempts, all 500, the error reaches the caller
        assertThatThrownBy(() -> client.reserve(REQUEST)).isInstanceOf(RestClientException.class).isNotInstanceOf(HttpClientErrorException.class);
        // 2nd call: its first attempt is the 4th failure in a window of 4, so the circuit opens and the retry is refused
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> client.reserve(REQUEST)).isInstanceOf(ResourceAccessException.class);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        inventory.verify(4, postRequestedFor(urlEqualTo(RESERVE)));
        assertThatThrownBy(() -> client.reserve(REQUEST)).isInstanceOf(ResourceAccessException.class).hasMessageContaining("inventory unavailable");
    }

    @Test
    void bulkheadRejectsCallsAboveItsLimitInsteadOfQueueing() throws Exception {
        inventory.stubFor(post(RESERVE).willReturn(okJson(RESERVATION).withFixedDelay(250)));
        InventoryClient client = client(2);

        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> calls = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(6)) {
            for (int i = 0; i < 6; i++) {
                calls.add(
                    executor.submit(() -> {
                        start.await();
                        try {
                            client.reserve(REQUEST);
                            return true;
                        } catch (ResourceAccessException e) {
                            return false;
                        }
                    })
                );
            }
            start.countDown();
            int succeeded = 0;
            for (Future<Boolean> call : calls) {
                succeeded += call.get(10, TimeUnit.SECONDS) ? 1 : 0;
            }
            assertThat(succeeded).isEqualTo(2);
        }
    }

    private InventoryClient client(int maxConcurrentCalls) {
        // Same request factory as ServiceClientsConfiguration
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(300));
        requestFactory.setReadTimeout(Duration.ofMillis(300));
        RestClient restClient = RestClient.builder().baseUrl(inventory.baseUrl()).requestFactory(requestFactory).build();
        InventoryClient target = HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build().createClient(InventoryClient.class);

        Retry retry = Retry.of(
            "inventory",
            RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(10))
                .retryExceptions(RestClientException.class)
                .ignoreExceptions(HttpClientErrorException.class)
                .build()
        );
        circuitBreaker = CircuitBreaker.of(
            "inventory",
            CircuitBreakerConfig.custom()
                .slidingWindowSize(4)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMinutes(1))
                .recordExceptions(RestClientException.class)
                .ignoreExceptions(HttpClientErrorException.class)
                .build()
        );
        Bulkhead bulkhead = Bulkhead.of(
            "inventory",
            BulkheadConfig.custom().maxConcurrentCalls(maxConcurrentCalls).maxWaitDuration(Duration.ZERO).build()
        );
        return ResilientClient.wrap(InventoryClient.class, target, retry, circuitBreaker, bulkhead);
    }
}
