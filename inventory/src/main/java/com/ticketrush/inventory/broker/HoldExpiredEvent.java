package com.ticketrush.inventory.broker;

import java.time.Instant;

/**
 * Published on topic {@code inventory.hold-expired.v1} when an order's seats went back on sale because its
 * hold ran out. The order service expires the order.
 */
public record HoldExpiredEvent(Long orderId, Instant expiredAt) {}
