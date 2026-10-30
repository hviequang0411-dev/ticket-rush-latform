package com.ticketrush.payment.web.rest;

import com.ticketrush.payment.service.flow.PaymentFlowService;
import com.ticketrush.payment.service.flow.PaymentRequest;
import com.ticketrush.payment.service.flow.PaymentView;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Payments for the order saga. Callers need {@code ROLE_SERVICE} (see SecurityConfiguration).
 * <ul>
 *   <li>409 when the order is already paid (create, cancel) or has no succeeded payment (refund)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/internal/payments")
public class InternalPaymentResource {

    private static final Logger LOG = LoggerFactory.getLogger(InternalPaymentResource.class);

    private final PaymentFlowService paymentFlowService;

    public InternalPaymentResource(PaymentFlowService paymentFlowService) {
        this.paymentFlowService = paymentFlowService;
    }

    /**
     * {@code POST /api/internal/payments} : open a payment for an order, or return the one already open.
     */
    @PostMapping("")
    public PaymentView create(@Valid @RequestBody PaymentRequest request) {
        LOG.debug("REST request to create payment : {}", request);
        return paymentFlowService.create(request);
    }

    /**
     * {@code POST /api/internal/payments/{orderId}/cancel} : abandon the order's open payment.
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<PaymentView> cancel(@PathVariable Long orderId) {
        LOG.debug("REST request to cancel payment of order : {}", orderId);
        return ResponseEntity.of(paymentFlowService.cancel(orderId));
    }

    /**
     * {@code POST /api/internal/payments/{orderId}/refund} : give the money of a paid order back.
     */
    @PostMapping("/{orderId}/refund")
    public PaymentView refund(@PathVariable Long orderId) {
        LOG.debug("REST request to refund payment of order : {}", orderId);
        return paymentFlowService.refund(orderId);
    }
}
