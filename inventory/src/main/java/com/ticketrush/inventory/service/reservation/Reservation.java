package com.ticketrush.inventory.service.reservation;

import com.ticketrush.inventory.domain.StockHold;
import com.ticketrush.inventory.domain.enumeration.HoldStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * The holds an order currently has, one line per ticket type.
 */
public record Reservation(Long orderId, Instant expiresAt, List<Line> lines) {
    public record Line(Long ticketTypeId, int quantity, HoldStatus status) {}

    static Reservation of(Long orderId, List<StockHold> holds) {
        Instant expiresAt = holds.stream().map(StockHold::getExpiresAt).min(Comparator.naturalOrder()).orElse(null);
        List<Line> lines = holds
            .stream()
            .map(hold -> new Line(hold.getStock().getTicketTypeId(), hold.getQuantity(), hold.getStatus()))
            .sorted(Comparator.comparing(Line::ticketTypeId))
            .toList();
        return new Reservation(orderId, expiresAt, lines);
    }
}
