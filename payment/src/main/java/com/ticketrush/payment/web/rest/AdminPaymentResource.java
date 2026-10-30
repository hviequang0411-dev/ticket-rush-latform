package com.ticketrush.payment.web.rest;

import com.ticketrush.payment.domain.Payment;
import com.ticketrush.payment.domain.PaymentWebhookEvent;
import com.ticketrush.payment.domain.enumeration.PaymentProvider;
import com.ticketrush.payment.domain.enumeration.PaymentStatus;
import com.ticketrush.payment.repository.PaymentRepository;
import com.ticketrush.payment.repository.PaymentWebhookEventRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * Payments and the provider callbacks behind them, for organisers ({@code /api/admin/**} is admins only).
 */
@RestController
@RequestMapping("/api/admin")
@Transactional(readOnly = true)
public class AdminPaymentResource {

    private final PaymentRepository paymentRepository;

    private final PaymentWebhookEventRepository webhookEventRepository;

    public AdminPaymentResource(PaymentRepository paymentRepository, PaymentWebhookEventRepository webhookEventRepository) {
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
    }

    /**
     * {@code GET /api/admin/payments?q=&status=} : newest first; {@code q} matches the order code.
     */
    @GetMapping("/payments")
    public ResponseEntity<List<PaymentRow>> payments(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) PaymentStatus status,
        @ParameterObject Pageable pageable
    ) {
        String pattern = "%" + (q == null ? "" : q.trim().toLowerCase(Locale.ROOT)) + "%";
        return paged(paymentRepository.search(pattern, status, pageable).map(PaymentRow::of));
    }

    /**
     * {@code GET /api/admin/webhooks} : every callback received, newest first, with how many times it was sent.
     */
    @GetMapping("/webhooks")
    public ResponseEntity<List<WebhookRow>> webhooks(@ParameterObject Pageable pageable) {
        return paged(webhookEventRepository.findAllNewestFirst(pageable).map(WebhookRow::of));
    }

    private static <T> ResponseEntity<List<T>> paged(Page<T> page) {
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    public record PaymentRow(
        Long id,
        Long orderId,
        String orderCode,
        BigDecimal amount,
        String currency,
        PaymentProvider provider,
        PaymentStatus status,
        String providerRef,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
    ) {
        static PaymentRow of(Payment p) {
            return new PaymentRow(
                p.getId(),
                p.getOrderId(),
                p.getOrderCode(),
                p.getAmount(),
                p.getCurrency(),
                p.getProvider(),
                p.getStatus(),
                p.getProviderRef(),
                p.getFailureReason(),
                p.getCreatedAt(),
                p.getUpdatedAt()
            );
        }
    }

    /**
     * @param outcome PROCESSED (handed to its payment, once however many deliveries), REJECTED (bad signature),
     *     UNMATCHED (no such payment) or PENDING (stored, not handled yet).
     */
    public record WebhookRow(
        Long id,
        PaymentProvider provider,
        String eventKey,
        String orderCode,
        BigDecimal amount,
        boolean signatureValid,
        String outcome,
        int deliveries,
        Instant receivedAt,
        Instant lastReceivedAt,
        Instant processedAt,
        String payload
    ) {
        static WebhookRow of(PaymentWebhookEvent e) {
            Payment payment = e.getPayment();
            String outcome;
            if (!Boolean.TRUE.equals(e.getSignatureValid())) {
                outcome = "REJECTED";
            } else if (e.getProcessedAt() == null) {
                outcome = "PENDING";
            } else {
                outcome = payment == null ? "UNMATCHED" : "PROCESSED";
            }
            return new WebhookRow(
                e.getId(),
                e.getProvider(),
                e.getEventKey(),
                payment != null ? payment.getOrderCode() : null,
                payment != null ? payment.getAmount() : null,
                Boolean.TRUE.equals(e.getSignatureValid()),
                outcome,
                e.getDeliveries(),
                e.getReceivedAt(),
                e.getLastReceivedAt(),
                e.getProcessedAt(),
                e.getPayload()
            );
        }
    }
}
