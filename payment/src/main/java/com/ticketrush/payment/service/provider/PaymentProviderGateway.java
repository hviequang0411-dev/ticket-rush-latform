package com.ticketrush.payment.service.provider;

import com.ticketrush.payment.domain.Payment;
import com.ticketrush.payment.domain.enumeration.PaymentProvider;
import java.math.BigDecimal;
import java.util.Map;

/**
 * One payment provider (mock, VNPay, MoMo). Implementations only translate between the provider's protocol and
 * these types; state changes and idempotency live in PaymentFlowService.
 */
public interface PaymentProviderGateway {
    PaymentProvider provider();

    /**
     * @return where to send the buyer to pay.
     */
    String createPaymentUrl(Payment payment);

    /**
     * Parses and verifies a callback. Never throws for a bad signature: the result says so, and the callback is
     * stored either way for auditing.
     */
    WebhookNotification parseWebhook(Map<String, String> params, String rawBody);

    /**
     * Returns the money of a succeeded payment. Called when seats could not be kept for an order that was paid.
     */
    void refund(Payment payment);

    /**
     * @param eventKey identifies this callback at the provider: a redelivery has the same key.
     * @param success whether the provider says the money was captured.
     */
    record WebhookNotification(
        boolean signatureValid,
        String eventKey,
        String providerRef,
        boolean success,
        String providerTransactionId,
        BigDecimal amount,
        String failureReason
    ) {}
}
