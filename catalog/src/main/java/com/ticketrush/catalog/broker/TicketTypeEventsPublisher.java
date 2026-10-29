package com.ticketrush.catalog.broker;

import com.ticketrush.catalog.service.event.TicketTypeChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes ticket type changes on {@code catalog.ticket-type.v1} once the change is committed, so inventory never
 * sees a ticket type that was rolled back. A crash between commit and send loses the event; phase 3 moves this to
 * a transactional outbox.
 */
@Component
public class TicketTypeEventsPublisher {

    static final String BINDING = "ticketTypeEvents-out-0";

    private static final Logger LOG = LoggerFactory.getLogger(TicketTypeEventsPublisher.class);

    private final StreamBridge streamBridge;

    public TicketTypeEventsPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @TransactionalEventListener
    public void onTicketTypeChanged(TicketTypeChanged event) {
        LOG.debug("Publishing ticket type event : {}", event);
        if (!streamBridge.send(BINDING, event)) {
            LOG.error("Could not publish ticket type event {}", event);
        }
    }
}
