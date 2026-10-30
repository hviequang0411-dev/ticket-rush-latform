package com.ticketrush.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

import com.ticketrush.gateway.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Payment providers call webhooks without a token or CSRF cookie. Only payment's public path may skip both; there
 * is no payment instance in the test, so a request that gets through ends as 503 instead of 401/403.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_TIMEOUT)
class PaymentPublicPathIT {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void providerCallbacksNeedNeitherTokenNorCsrf() {
        HttpStatus status = HttpStatus.valueOf(
            webTestClient.post().uri("/services/payment/api/public/webhooks/mock?ref=X").exchange().returnResult(Void.class).getStatus().value()
        );

        assertThat(status).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN, HttpStatus.FOUND);
    }

    @Test
    void everythingElseStillNeedsCsrf() {
        webTestClient
            .mutateWith(mockUser("alice"))
            .post()
            .uri("/services/payment/api/internal/payments")
            .exchange()
            .expectStatus()
            .isForbidden();
    }

    @Test
    void bearerTokenRequestsAreNotSubjectToCsrf() {
        // CSRF runs first and would answer 403; past it, the test's mocked JWT decoder decides (not 403 either way)
        webTestClient
            .post()
            .uri("/services/order/api/checkout")
            .header("Authorization", "Bearer not-a-real-token")
            .exchange()
            .expectStatus()
            .value(status -> assertThat(status).isNotEqualTo(HttpStatus.FORBIDDEN.value()));
    }

    @Test
    void everythingElseStillNeedsAuthentication() {
        webTestClient.get().uri("/services/payment/api/payments").exchange().expectStatus().value(status -> assertThat(status).isIn(302, 401));
    }
}
