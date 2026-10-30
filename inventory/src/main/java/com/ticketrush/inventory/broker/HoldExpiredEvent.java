package com.ticketrush.inventory.broker;

import java.time.Instant;

/**
 * Published on {@value #TOPIC} through the outbox, keyed by order id, when an order's seats went back on sale
 * because its hold ran out. The order service expires the order.
 */
public record HoldExpiredEvent(Long orderId, Instant expiredAt) {
    public static final String TOPIC = "inventory.hold-expired.v1";
}
