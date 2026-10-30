package com.ticketrush.order.domain.enumeration;

/**
 * The OrderStatus enumeration.
 */
public enum OrderStatus {
    PENDING,
    RESERVED,
    AWAITING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    EXPIRED,
    FAILED,
    REFUNDED,
}
