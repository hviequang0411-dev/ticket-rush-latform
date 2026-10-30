package com.ticketrush.payment.service.flow;

import com.ticketrush.payment.broker.PaymentResultEvent;
import com.ticketrush.payment.domain.Payment;
import com.ticketrush.payment.domain.PaymentWebhookEvent;
import com.ticketrush.payment.domain.enumeration.PaymentProvider;
import com.ticketrush.payment.domain.enumeration.PaymentStatus;
import com.ticketrush.payment.repository.PaymentRepository;
import com.ticketrush.payment.repository.PaymentWebhookEventRepository;
import com.ticketrush.payment.service.outbox.Outbox;
import com.ticketrush.payment.service.provider.PaymentProviderGateway;
import com.ticketrush.payment.service.provider.PaymentProviderGateway.WebhookNotification;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

/**
 * Payment lifecycle: create for an order, apply provider callbacks, cancel, refund, expire.
 * <p>
 * Every final result the order saga cares about is published on {@link PaymentResultEvent#TOPIC} through the
 * outbox, in the transaction that changed the payment. Callbacks are applied exactly once even when providers
 * deliver them several times or concurrently: the inbox stores each callback once, and the payment row is locked
 * while a result is applied, which only ever moves a payment out of PENDING.
 */
@Service
public class PaymentFlowService {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentFlowService.class);

    private static final char[] REF_ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    private final PaymentRepository paymentRepository;

    private final PaymentWebhookEventRepository webhookEventRepository;

    private final WebhookInbox webhookInbox;

    private final Outbox outbox;

    private final TransactionTemplate transactionTemplate;

    private final ObjectMapper objectMapper;

    private final Clock clock;

    private final Map<PaymentProvider, PaymentProviderGateway> gateways = new EnumMap<>(PaymentProvider.class);

    private final PaymentProvider defaultProvider;

    private final int expiryBatchSize;

    private final SecureRandom random = new SecureRandom();

    public PaymentFlowService(
        PaymentRepository paymentRepository,
        PaymentWebhookEventRepository webhookEventRepository,
        WebhookInbox webhookInbox,
        Outbox outbox,
        TransactionTemplate transactionTemplate,
        ObjectMapper objectMapper,
        Clock clock,
        List<PaymentProviderGateway> gateways,
        @Value("${ticketrush.payment.provider:MOCK}") PaymentProvider defaultProvider,
        @Value("${ticketrush.payment.expiry-batch-size:100}") int expiryBatchSize
    ) {
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.webhookInbox = webhookInbox;
        this.outbox = outbox;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
        gateways.forEach(gateway -> this.gateways.put(gateway.provider(), gateway));
        if (!this.gateways.containsKey(defaultProvider)) {
            throw new IllegalStateException("No gateway for payment provider " + defaultProvider);
        }
        this.defaultProvider = defaultProvider;
        this.expiryBatchSize = expiryBatchSize;
    }

    /**
     * Opens a payment for an order, or returns the one already open. Idempotent per order.
     *
     * @throws PaymentStateException if the order was already paid (or paid and refunded).
     */
    public PaymentView create(PaymentRequest request) {
        LOG.debug("Request to create payment : {}", request);
        try {
            return transactionTemplate.execute(status -> createOrReuse(request));
        } catch (DataIntegrityViolationException e) {
            // A concurrent request opened the payment first (one PENDING payment per order is a unique index)
            return transactionTemplate.execute(status ->
                paymentRepository.findFirstByOrderIdAndStatus(request.orderId(), PaymentStatus.PENDING).map(PaymentView::of).orElseThrow(() -> e)
            );
        }
    }

    /**
     * Abandons the open payment of an order (order cancelled or its seats expired). Idempotent.
     *
     * @throws PaymentStateException if the order was paid meanwhile; the saga then refunds instead.
     */
    public Optional<PaymentView> cancel(Long orderId) {
        return transactionTemplate.execute(status -> {
            List<Payment> payments = paymentRepository.lockByOrderId(orderId);
            if (payments.stream().anyMatch(payment -> payment.getStatus() == PaymentStatus.SUCCEEDED)) {
                throw new PaymentStateException("Order " + orderId + " is already paid");
            }
            payments
                .stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .forEach(payment -> payment.status(PaymentStatus.EXPIRED).failureReason("CANCELLED").updatedAt(clock.instant()));
            return payments.stream().findFirst().map(PaymentView::of);
        });
    }

    /**
     * Gives the money of a paid order back. Idempotent.
     *
     * @throws PaymentStateException if the order has no succeeded payment.
     */
    public PaymentView refund(Long orderId) {
        return transactionTemplate.execute(status -> {
            List<Payment> payments = paymentRepository.lockByOrderId(orderId);
            Optional<Payment> refunded = payments.stream().filter(payment -> payment.getStatus() == PaymentStatus.REFUNDED).findFirst();
            if (refunded.isPresent()) {
                return PaymentView.of(refunded.orElseThrow());
            }
            Payment paid = payments
                .stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.SUCCEEDED)
                .findFirst()
                .orElseThrow(() -> new PaymentStateException("Order " + orderId + " has no succeeded payment"));
            gateway(paid.getProvider()).refund(paid);
            LOG.info("Refunded payment {} of order {}", paid.getProviderRef(), paid.getOrderCode());
            return PaymentView.of(paid.status(PaymentStatus.REFUNDED).updatedAt(clock.instant()));
        });
    }

    /**
     * Stores, verifies and applies a provider callback.
     *
     * @throws InvalidWebhookException if the signature does not verify.
     */
    public void handleWebhook(PaymentProvider provider, Map<String, String> params, String rawBody) {
        WebhookNotification notification = gateway(provider).parseWebhook(params, rawBody);
        String payload = rawBody != null && !rawBody.isBlank() ? rawBody : objectMapper.writeValueAsString(params);
        PaymentWebhookEvent received = webhookInbox.record(provider, notification.eventKey(), payload, notification.signatureValid());
        if (!notification.signatureValid()) {
            LOG.warn("Rejected {} callback {} with an invalid signature", provider, notification.eventKey());
            throw new InvalidWebhookException("Invalid signature");
        }
        if (received.getProcessedAt() != null) {
            LOG.debug("Callback {} was already applied", notification.eventKey());
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            PaymentWebhookEvent event = webhookEventRepository.findById(received.getId()).orElseThrow();
            Optional<Payment> payment = paymentRepository.lockByProviderRef(notification.providerRef());
            payment.ifPresentOrElse(
                p -> {
                    apply(p, notification);
                    event.payment(p);
                },
                () -> LOG.warn("Callback {} for unknown payment {}", notification.eventKey(), notification.providerRef())
            );
            event.processedAt(clock.instant());
        });
    }

    /**
     * Expires one batch of payments nobody completed in time.
     *
     * @return how many were expired; fewer than the batch size means none are left.
     */
    public int expireDue() {
        Integer expired = transactionTemplate.execute(status -> {
            List<Payment> due = paymentRepository.lockExpiredPending(clock.instant(), expiryBatchSize);
            due.forEach(payment -> {
                payment.status(PaymentStatus.EXPIRED).failureReason("TIMEOUT").updatedAt(clock.instant());
                publish(payment, false);
            });
            return due.size();
        });
        return expired == null ? 0 : expired;
    }

    public Optional<PaymentView> findByProviderRef(String providerRef) {
        return paymentRepository.findByProviderRef(providerRef).map(PaymentView::of);
    }

    private PaymentView createOrReuse(PaymentRequest request) {
        List<Payment> payments = paymentRepository.lockByOrderId(request.orderId());
        for (Payment payment : payments) {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                return PaymentView.of(payment);
            }
            if (payment.getStatus() == PaymentStatus.SUCCEEDED || payment.getStatus() == PaymentStatus.REFUNDED) {
                throw new PaymentStateException("Order " + request.orderId() + " was already paid");
            }
        }
        Instant now = clock.instant();
        Payment payment = new Payment()
            .orderId(request.orderId())
            .orderCode(request.orderCode())
            .amount(request.amount())
            .currency(request.currency())
            .provider(defaultProvider)
            .status(PaymentStatus.PENDING)
            .providerRef(newProviderRef())
            .expiresAt(request.expiresAt())
            .createdAt(now);
        payment.paymentUrl(gateway(defaultProvider).createPaymentUrl(payment));
        return PaymentView.of(paymentRepository.saveAndFlush(payment));
    }

    private void apply(Payment payment, WebhookNotification notification) {
        boolean open = payment.getStatus() == PaymentStatus.PENDING;
        boolean lateSuccess = payment.getStatus() == PaymentStatus.EXPIRED && notification.success();
        if (!open && !lateSuccess) {
            LOG.info("Ignoring callback {} for payment {} that is already {}", notification.eventKey(), payment.getProviderRef(), payment.getStatus());
            return;
        }
        if (notification.amount() == null || notification.amount().compareTo(payment.getAmount()) != 0) {
            LOG.error("Callback {} amount {} does not match payment {} amount {}", notification.eventKey(), notification.amount(), payment.getProviderRef(), payment.getAmount());
            if (open) {
                payment.status(PaymentStatus.FAILED).failureReason("AMOUNT_MISMATCH").updatedAt(clock.instant());
                publish(payment, false);
            }
            return;
        }
        payment.providerTransactionId(notification.providerTransactionId()).updatedAt(clock.instant());
        if (notification.success()) {
            payment.status(PaymentStatus.SUCCEEDED).failureReason(null);
        } else {
            payment.status(PaymentStatus.FAILED).failureReason(notification.failureReason());
        }
        LOG.info("Payment {} of order {} is {}{}", payment.getProviderRef(), payment.getOrderCode(), payment.getStatus(), lateSuccess ? " (late)" : "");
        publish(payment, lateSuccess);
    }

    private void publish(Payment payment, boolean late) {
        outbox.append(
            PaymentResultEvent.TOPIC,
            "Order",
            payment.getOrderId(),
            "PAYMENT_" + payment.getStatus().name(),
            new PaymentResultEvent(
                payment.getOrderId(),
                payment.getOrderCode(),
                payment.getProviderRef(),
                payment.getStatus().name(),
                payment.getAmount(),
                payment.getCurrency(),
                late,
                payment.getFailureReason(),
                clock.instant()
            )
        );
    }

    private PaymentProviderGateway gateway(PaymentProvider provider) {
        PaymentProviderGateway gateway = gateways.get(provider);
        if (gateway == null) {
            throw new IllegalStateException("No gateway for payment provider " + provider);
        }
        return gateway;
    }

    private String newProviderRef() {
        char[] ref = new char[16];
        for (int i = 0; i < ref.length; i++) {
            ref[i] = REF_ALPHABET[random.nextInt(REF_ALPHABET.length)];
        }
        return "PAY" + new String(ref);
    }
}
