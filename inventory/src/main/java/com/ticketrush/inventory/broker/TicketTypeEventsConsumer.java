package com.ticketrush.inventory.broker;

import com.ticketrush.inventory.service.StockSyncService;
import java.util.UUID;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Keeps TicketStock in step with catalog. Bound to {@code ticketTypeEvents-in-0}.
 */
@Component("ticketTypeEvents")
public class TicketTypeEventsConsumer implements Consumer<Message<TicketTypeEvent>> {

    /** Header set by the publisher's outbox relay. */
    static final String EVENT_ID = "eventId";

    private static final Logger LOG = LoggerFactory.getLogger(TicketTypeEventsConsumer.class);

    private final StockSyncService stockSyncService;

    public TicketTypeEventsConsumer(StockSyncService stockSyncService) {
        this.stockSyncService = stockSyncService;
    }

    @Override
    public void accept(Message<TicketTypeEvent> message) {
        LOG.debug("Received ticket type event : {}", message.getPayload());
        Object eventId = message.getHeaders().get(EVENT_ID);
        stockSyncService.apply(message.getPayload(), eventId == null ? null : UUID.fromString(eventId.toString()));
    }
}
