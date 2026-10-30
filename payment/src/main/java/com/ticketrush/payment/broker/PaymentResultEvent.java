package com.ticketrush.payment.broker;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published on {@value #TOPIC} through the outbox, keyed by order id, when a payment reaches a final state the
 * order saga must react to.
 *
 * @param status SUCCEEDED, FAILED or EXPIRED.
 * @param late true when the money arrived after the payment had already expired; the saga refunds it unless it
 *     can still keep the seats.
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
