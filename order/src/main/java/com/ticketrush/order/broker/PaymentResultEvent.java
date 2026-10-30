package com.ticketrush.order.broker;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Consumed from {@value #TOPIC}: a payment reached SUCCEEDED, FAILED or EXPIRED. {@code late} means the money
 * arrived after the payment had expired.
 */
public record PaymentResultEvent(
    Long orderId,
    String orderCode,
    String providerRef,
    String status,
    BigDecimal amount,
    String currency,
    boolean late,
    String failureReason,
    Instant occurredAt
) {
    public static final String TOPIC = "payment.result.v1";
}
