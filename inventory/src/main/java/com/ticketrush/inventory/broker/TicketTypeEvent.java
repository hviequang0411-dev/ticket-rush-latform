package com.ticketrush.inventory.broker;

import java.time.Instant;

/**
 * Published by catalog on topic {@code catalog.ticket-type.v1} whenever a ticket type is created or its
 * quantity changes. {@code totalQuantity} is the new absolute value, so applying an event twice is harmless.
 */
public record TicketTypeEvent(Type type, Long ticketTypeId, Long eventId, Integer totalQuantity, Instant occurredAt) {
    public enum Type {
        CREATED,
        UPDATED,
    }
}
