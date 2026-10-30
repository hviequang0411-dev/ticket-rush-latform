package com.ticketrush.order.broker;

import java.util.UUID;
import org.springframework.messaging.Message;

/**
 * Reads the event id the publisher's outbox relay puts on every message.
 */
final class EventIds {

    static final String HEADER = "eventId";

    private EventIds() {}

    static UUID of(Message<?> message) {
        Object id = message.getHeaders().get(HEADER);
        return id == null ? null : UUID.fromString(id.toString());
    }
}
