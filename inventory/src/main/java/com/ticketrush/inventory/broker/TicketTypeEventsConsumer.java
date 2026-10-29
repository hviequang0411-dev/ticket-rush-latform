package com.ticketrush.inventory.broker;

import com.ticketrush.inventory.service.StockSyncService;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Keeps TicketStock in step with catalog. Bound to {@code ticketTypeEvents-in-0}.
 */
@Component("ticketTypeEvents")
public class TicketTypeEventsConsumer implements Consumer<TicketTypeEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(TicketTypeEventsConsumer.class);

    private final StockSyncService stockSyncService;

    public TicketTypeEventsConsumer(StockSyncService stockSyncService) {
        this.stockSyncService = stockSyncService;
    }

    @Override
    public void accept(TicketTypeEvent event) {
        LOG.debug("Received ticket type event : {}", event);
        stockSyncService.apply(event);
    }
}
