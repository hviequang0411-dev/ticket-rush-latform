package com.ticketrush.order.broker;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published on {@value #TOPIC} through the outbox, keyed by order id, whenever an order reaches a final state
 * (CONFIRMED, CANCELLED, EXPIRED, FAILED, REFUNDED). Notifications and analytics consume it.
 */
public record OrderEvent(
    Long orderId,
    String code,
    String userLogin,
    String status,
    BigDecimal totalAmount,
    String currency,
    String failureReason,
    Instant occurredAt
) {
    public static final String TOPIC = "order.events.v1";
}
