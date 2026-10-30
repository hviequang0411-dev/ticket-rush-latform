package com.ticketrush.catalog.service.event;

import com.ticketrush.catalog.domain.TicketType;
import com.ticketrush.catalog.service.outbox.Outbox;
import java.time.Clock;
import org.springframework.stereotype.Component;

/**
 * Tells inventory about new and resized ticket types, through the outbox and in the caller's transaction. Every
 * write to a ticket type goes through here, whichever API made it.
 */
@Component
public class TicketTypeChanges {

    private final Outbox outbox;

    private final Clock clock;

    public TicketTypeChanges(Outbox outbox, Clock clock) {
        this.outbox = outbox;
        this.clock = clock;
    }

    public void raise(TicketTypeChanged.Type type, TicketType ticketType) {
        Long eventId = ticketType.getEvent() != null ? ticketType.getEvent().getId() : null;
        outbox.append(
            TicketTypeChanged.TOPIC,
            "TicketType",
            ticketType.getId(),
            type.name(),
            new TicketTypeChanged(type, ticketType.getId(), eventId, ticketType.getTotalQuantity(), clock.instant())
        );
    }
}
