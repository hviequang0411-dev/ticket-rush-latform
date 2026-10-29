package com.ticketrush.order.broker;

import java.time.Instant;

/**
 * Consumed from {@code inventory.hold-expired.v1}: the order's seats went back on sale.
 */
public record HoldExpiredEvent(Long orderId, Instant expiredAt) {}
