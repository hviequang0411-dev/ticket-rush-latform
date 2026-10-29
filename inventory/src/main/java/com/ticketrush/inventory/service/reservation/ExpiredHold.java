package com.ticketrush.inventory.service.reservation;

import java.time.Instant;

/**
 * A hold the expiry job released, reported so the owning order can be expired.
 */
public record ExpiredHold(Long orderId, Long ticketTypeId, int quantity, Instant expiredAt) {}
