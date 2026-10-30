package com.ticketrush.gateway.service.waitingroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockUser;

import com.ticketrush.gateway.IntegrationTest;
import com.ticketrush.gateway.security.AuthoritiesConstants;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * The queue against real Redis, with a clock the test moves: 20 admissions per second in the default config.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_TIMEOUT)
@Import(WaitingRoomIT.TestClock.class)
class WaitingRoomIT {

    static final AtomicReference<Instant> NOW = new AtomicReference<>(Instant.parse("2030-01-01T00:00:00Z"));

    @TestConfiguration
    static class TestClock {

        @Bean
        @Primary
        Clock testClock() {
            return new Clock() {
                @Override
                public ZoneOffset getZone() {
                    return ZoneOffset.UTC;
                }

                @Override
                public Clock withZone(java.time.ZoneId zone) {
                    return this;
                }

                @Override
                public Instant instant() {
                    return NOW.get();
                }
            };
        }
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private WaitingRoomService waitingRoomService;

    @Test
    void buyersAreAdmittedInOrderAtTheConfiguredRate() {
        long event = System.nanoTime();
        for (int i = 0; i < 50; i++) {
            waitingRoomService.join(event, "buyer-" + i).block();
        }
        // Rejoining keeps your place
        assertThat(waitingRoomService.join(event, "buyer-0").block().ahead()).isZero();
        WaitingRoomService.QueueStatus last = waitingRoomService.status(event, "buyer-49").block();
        assertThat(last.admitted()).isFalse();
        assertThat(last.ahead()).isEqualTo(49);
        assertThat(last.etaSeconds()).isEqualTo(3);

        tick(Duration.ofSeconds(1));
        assertThat(waitingRoomService.status(event, "buyer-19").block().admitted()).isTrue();
        WaitingRoomService.QueueStatus next = waitingRoomService.status(event, "buyer-20").block();
        assertThat(next.admitted()).isFalse();
        assertThat(next.ahead()).isZero();

        tick(Duration.ofSeconds(2));
        WaitingRoomService.QueueStatus admitted = waitingRoomService.status(event, "buyer-49").block();
        assertThat(admitted.admitted()).isTrue();
        assertThat(admitted.pass()).startsWith(event + ".");
        assertThat(waitingRoomService.stats(event).block().waiting()).isZero();
    }

    @Test
    void anIdleQueueDoesNotSaveUpAdmissions() {
        long event = System.nanoTime();
        waitingRoomService.join(event, "early").block();
        tick(Duration.ofMinutes(10));
        assertThat(waitingRoomService.status(event, "early").block().admitted()).isTrue();

        for (int i = 0; i < 30; i++) {
            waitingRoomService.join(event, "late-" + i).block();
        }
        // Ten quiet minutes don't let 12,000 people straight in
        assertThat(waitingRoomService.status(event, "late-29").block().admitted()).isFalse();
    }

    @Test
    void joiningNeedsSignInAndStatsNeedAdmin() {
        long event = System.nanoTime();
        webTestClient.mutateWith(csrf()).post().uri("/api/waiting-room/" + event).exchange().expectStatus().isUnauthorized();
        webTestClient
            .mutateWith(mockUser("alice"))
            .mutateWith(csrf())
            .post()
            .uri("/api/waiting-room/" + event)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.inQueue")
            .isEqualTo(true);
        webTestClient.mutateWith(mockUser("alice")).get().uri("/api/admin/waiting-room/" + event).exchange().expectStatus().isForbidden();
        webTestClient
            .mutateWith(mockUser("boss").authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(AuthoritiesConstants.ADMIN)))
            .get()
            .uri("/api/admin/waiting-room/" + event)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.joined")
            .isEqualTo(1);
    }

    private static void tick(Duration duration) {
        NOW.updateAndGet(now -> now.plus(duration));
    }
}
