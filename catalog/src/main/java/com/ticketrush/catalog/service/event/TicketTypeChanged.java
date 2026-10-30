package com.ticketrush.catalog.service.event;

import java.time.Instant;

/**
 * Published on {@value #TOPIC} through the outbox whenever a ticket type is created or updated, keyed by ticket
 * type id. {@code totalQuantity} is the new absolute value.
 */
public record TicketTypeChanged(Type type, Long ticketTypeId, Long eventId, Integer totalQuantity, Instant occurredAt) {
    public static final String TOPIC = "catalog.ticket-type.v1";

    public enum Type {
        CREATED,
        UPDATED,
    }
}
