package com.ticketrush.gateway.web.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

import com.ticketrush.gateway.IntegrationTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * The checkout route's token bucket (3 requests per burst, one more every 20 s in the test config) in front of
 * real Redis. There is no
 * order instance in the test, so requests that get through the limiter end as 503; the point is which ones are
 * stopped with 429 before reaching it.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_TIMEOUT)
class CheckoutRateLimitIT {

    private static final String CHECKOUT = "/services/order/api/checkout";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void burstAboveTheLimitIsRejectedPerUser() {
        List<HttpStatus> alice = fire("alice-" + System.nanoTime(), 6);

        assertThat(alice.subList(0, 3)).doesNotContain(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(alice.subList(3, 6)).containsOnly(HttpStatus.TOO_MANY_REQUESTS);

        // Another buyer has a bucket of their own
        assertThat(fire("bob-" + System.nanoTime(), 1)).doesNotContain(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    void rejectedResponsesTellClientsTheLimit() {
        String user = "carol-" + System.nanoTime();
        fire(user, 3);

        webTestClient
            .mutateWith(mockUser(user))
            .mutateWith(csrf())
            .post()
            .uri(CHECKOUT)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
            .expectHeader()
            .valueEquals("X-RateLimit-Burst-Capacity", "60")
            .expectHeader()
            .valueEquals("X-RateLimit-Requested-Tokens", "20")
            .expectHeader()
            .value("X-RateLimit-Remaining", remaining -> assertThat(Integer.parseInt(remaining)).isLessThan(20));
    }

    @Test
    void otherRoutesAreNotLimitedByTheCheckoutBucket() {
        String user = "dave-" + System.nanoTime();
        fire(user, 4);

        webTestClient
            .mutateWith(mockUser(user))
            .get()
            .uri("/services/order/api/my-orders")
            .exchange()
            .expectStatus()
            .value(status -> assertThat(status).isNotEqualTo(HttpStatus.TOO_MANY_REQUESTS.value()));
    }

    private List<HttpStatus> fire(String user, int requests) {
        List<HttpStatus> statuses = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            statuses.add(
                HttpStatus.valueOf(
                    webTestClient
                        .mutateWith(mockUser(user))
                        .mutateWith(csrf())
                        .post()
                        .uri(CHECKOUT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue("{}")
                        .exchange()
                        .returnResult(Void.class)
                        .getStatus()
                        .value()
                )
            );
        }
        return statuses;
    }
}
