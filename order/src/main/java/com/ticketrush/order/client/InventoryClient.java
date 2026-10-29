package com.ticketrush.order.client;

import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Seat holds in the inventory service. Responds 409 when seats are sold out or holds are no longer active.
 */
@HttpExchange("/api/internal/reservations")
public interface InventoryClient {
    @PostExchange
    Reservation reserve(@RequestBody ReserveRequest request);

    @PostExchange("/{orderId}/confirm")
    Reservation confirm(@PathVariable Long orderId);

    @PostExchange("/{orderId}/release")
    Reservation release(@PathVariable Long orderId);

    record ReserveRequest(Long orderId, List<Item> items) {
        public record Item(Long ticketTypeId, int quantity) {}
    }

    record Reservation(Long orderId, Instant expiresAt, List<Line> lines) {
        public record Line(Long ticketTypeId, int quantity, String status) {}
    }
}
