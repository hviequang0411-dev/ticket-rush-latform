package com.ticketrush.payment.service.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketrush.payment.IntegrationTest;
import com.ticketrush.payment.domain.OutboxEvent;
import com.ticketrush.payment.domain.enumeration.PaymentProvider;
import com.ticketrush.payment.domain.enumeration.PaymentStatus;
import com.ticketrush.payment.repository.OutboxEventRepository;
import com.ticketrush.payment.repository.PaymentRepository;
import com.ticketrush.payment.repository.PaymentWebhookEventRepository;
import com.ticketrush.payment.service.provider.MockPaymentProvider;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest
@Import(PaymentFlowServiceIT.ClockConfig.class)
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=12")
class PaymentFlowServiceIT {

    private static final AtomicLong ORDER_IDS = new AtomicLong(5_000);

    private static final BigDecimal AMOUNT = new BigDecimal("2500000");

    @TestConfiguration
    static class ClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    @Autowired
    private PaymentFlowService paymentFlowService;

    @Autowired
    private MockPaymentProvider mockProvider;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentWebhookEventRepository webhookEventRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock clock;

    @AfterEach
    void cleanup() {
        webhookEventRepository.deleteAll();
        paymentRepository.deleteAll();
        outboxEventRepository.deleteAll();
    }

    @Test
    void createIsIdempotentPerOrder() {
        PaymentRequest request = request();

        PaymentView first = paymentFlowService.create(request);
        PaymentView again = paymentFlowService.create(request);

        assertThat(again.providerRef()).isEqualTo(first.providerRef());
        assertThat(first.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(first.paymentUrl()).endsWith("/services/payment/api/public/mock-provider/" + first.providerRef());
        assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    void successfulCallbackIsAppliedAndPublishedOnce() throws Exception {
        PaymentView payment = paymentFlowService.create(request());
        Map<String, String> callback = mockProvider.callbackFor(payment.providerRef(), AMOUNT, MockPaymentProvider.SUCCESS);

        paymentFlowService.handleWebhook(PaymentProvider.MOCK, callback, null);
        paymentFlowService.handleWebhook(PaymentProvider.MOCK, callback, null);
        // A different, later callback for an already-final payment changes nothing either
        paymentFlowService.handleWebhook(PaymentProvider.MOCK, mockProvider.callbackFor(payment.providerRef(), AMOUNT, "declined"), null);

        assertThat(status(payment)).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(results()).singleElement().satisfies(event -> {
            assertThat(event.getEventType()).isEqualTo("PAYMENT_SUCCEEDED");
            assertThat(event.getAggregateId()).isEqualTo(String.valueOf(payment.orderId()));
            assertThat(objectMapper.readTree(event.getPayload()).get("late").asBoolean()).isFalse();
        });
        assertThat(webhookEventRepository.findAll()).hasSize(2).allMatch(event -> event.getProcessedAt() != null);
    }

    @Test
    void concurrentDuplicateCallbacksPublishOneResult() throws Exception {
        PaymentView payment = paymentFlowService.create(request());
        Map<String, String> callback = mockProvider.callbackFor(payment.providerRef(), AMOUNT, MockPaymentProvider.SUCCESS);

        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> deliveries = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
            for (int i = 0; i < 10; i++) {
                deliveries.add(
                    executor.submit(() -> {
                        start.await();
                        paymentFlowService.handleWebhook(PaymentProvider.MOCK, callback, null);
                        return null;
                    })
                );
            }
            start.countDown();
            for (Future<?> delivery : deliveries) {
                delivery.get(60, TimeUnit.SECONDS);
            }
        }

        assertThat(status(payment)).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(results()).hasSize(1);
        assertThat(webhookEventRepository.count()).isEqualTo(1);
    }

    @Test
    void forgedCallbackIsStoredButNeverApplied() {
        PaymentView payment = paymentFlowService.create(request());
        Map<String, String> forged = new HashMap<>(mockProvider.callbackFor(payment.providerRef(), AMOUNT, MockPaymentProvider.SUCCESS));
        forged.put("signature", "00".repeat(32));

        assertThatThrownBy(() -> paymentFlowService.handleWebhook(PaymentProvider.MOCK, forged, null)).isInstanceOf(InvalidWebhookException.class);

        assertThat(status(payment)).isEqualTo(PaymentStatus.PENDING);
        assertThat(results()).isEmpty();
        assertThat(webhookEventRepository.findAll()).singleElement().satisfies(event -> assertThat(event.getSignatureValid()).isFalse());
    }

    @Test
    void tamperedAmountFailsThePayment() {
        PaymentView payment = paymentFlowService.create(request());

        paymentFlowService.handleWebhook(PaymentProvider.MOCK, mockProvider.callbackFor(payment.providerRef(), new BigDecimal("1000"), MockPaymentProvider.SUCCESS), null);

        assertThat(paymentRepository.findByProviderRef(payment.providerRef()).orElseThrow().getFailureReason()).isEqualTo("AMOUNT_MISMATCH");
        assertThat(results()).singleElement().extracting(OutboxEvent::getEventType).isEqualTo("PAYMENT_FAILED");
    }

    @Test
    void declinedPaymentPublishesFailure() {
        PaymentView payment = paymentFlowService.create(request());

        paymentFlowService.handleWebhook(PaymentProvider.MOCK, mockProvider.callbackFor(payment.providerRef(), AMOUNT, "declined"), null);

        assertThat(status(payment)).isEqualTo(PaymentStatus.FAILED);
        assertThat(results()).singleElement().extracting(OutboxEvent::getEventType).isEqualTo("PAYMENT_FAILED");
    }

    @Test
    void expiredPaymentThatIsPaidLateIsReportedAsLate() throws Exception {
        PaymentView payment = paymentFlowService.create(request());
        ((MutableClock) clock).advance(Duration.ofMinutes(11));

        assertThat(paymentFlowService.expireDue()).isEqualTo(1);
        assertThat(paymentFlowService.expireDue()).isZero();
        assertThat(status(payment)).isEqualTo(PaymentStatus.EXPIRED);

        paymentFlowService.handleWebhook(PaymentProvider.MOCK, mockProvider.callbackFor(payment.providerRef(), AMOUNT, MockPaymentProvider.SUCCESS), null);

        assertThat(status(payment)).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(results()).extracting(OutboxEvent::getEventType).containsExactly("PAYMENT_EXPIRED", "PAYMENT_SUCCEEDED");
        assertThat(objectMapper.readTree(results().get(1).getPayload()).get("late").asBoolean()).isTrue();
    }

    @Test
    void cancelAbandonsAnOpenPaymentButNotAPaidOne() {
        PaymentView open = paymentFlowService.create(request());
        paymentFlowService.cancel(open.orderId());
        assertThat(status(open)).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(results()).isEmpty();

        PaymentView paid = paymentFlowService.create(request());
        paymentFlowService.handleWebhook(PaymentProvider.MOCK, mockProvider.callbackFor(paid.providerRef(), AMOUNT, MockPaymentProvider.SUCCESS), null);
        assertThatThrownBy(() -> paymentFlowService.cancel(paid.orderId())).isInstanceOf(PaymentStateException.class);
    }

    @Test
    void refundIsIdempotentAndOnlyForPaidOrders() {
        PaymentView payment = paymentFlowService.create(request());
        assertThatThrownBy(() -> paymentFlowService.refund(payment.orderId())).isInstanceOf(PaymentStateException.class);

        paymentFlowService.handleWebhook(PaymentProvider.MOCK, mockProvider.callbackFor(payment.providerRef(), AMOUNT, MockPaymentProvider.SUCCESS), null);
        assertThat(paymentFlowService.refund(payment.orderId()).status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(paymentFlowService.refund(payment.orderId()).status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThatThrownBy(() -> paymentFlowService.create(new PaymentRequest(payment.orderId(), "TRX", AMOUNT, "VND", clock.instant())))
            .as("a refunded order is not charged again")
            .isInstanceOf(PaymentStateException.class);
    }

    private PaymentStatus status(PaymentView payment) {
        return paymentRepository.findByProviderRef(payment.providerRef()).orElseThrow().getStatus();
    }

    private List<OutboxEvent> results() {
        return outboxEventRepository
            .findAll()
            .stream()
            .sorted(java.util.Comparator.comparing(OutboxEvent::getCreatedAt).thenComparing(OutboxEvent::getEventType))
            .toList();
    }

    private PaymentRequest request() {
        long orderId = ORDER_IDS.incrementAndGet();
        return new PaymentRequest(orderId, "TR" + orderId, AMOUNT, "VND", clock.instant().plus(Duration.ofMinutes(10)));
    }
}
