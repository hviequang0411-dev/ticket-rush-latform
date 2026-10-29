package com.ticketrush.order.client;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Ticket types in the catalog service; the source of truth for prices.
 */
@HttpExchange("/api/ticket-types")
public interface CatalogClient {
    @GetExchange("/{id}")
    TicketType getTicketType(@PathVariable Long id);

    record TicketType(Long id, String name, BigDecimal price, String currency, Instant saleStart, Instant saleEnd, EventRef event) {
        public Long eventId() {
            return event != null ? event.id() : null;
        }
    }

    record EventRef(Long id) {}
}
