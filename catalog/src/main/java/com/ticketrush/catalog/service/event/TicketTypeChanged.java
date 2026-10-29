package com.ticketrush.catalog.service.event;

import java.time.Instant;

/**
 * Raised inside the transaction that created or updated a ticket type. Forwarded to Kafka only after commit
 * (see {@link com.ticketrush.catalog.broker.TicketTypeEventsPublisher}).
 */
public record TicketTypeChanged(Type type, Long ticketTypeId, Long eventId, Integer totalQuantity, Instant occurredAt) {
    public enum Type {
        CREATED,
        UPDATED,
    }
}
