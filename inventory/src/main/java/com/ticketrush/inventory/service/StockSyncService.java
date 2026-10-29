package com.ticketrush.inventory.service;

import com.ticketrush.inventory.broker.TicketTypeEvent;
import com.ticketrush.inventory.domain.TicketStock;
import com.ticketrush.inventory.repository.TicketStockRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Applies catalog's ticket type quantities to TicketStock.
 * <p>
 * Idempotent: events carry the absolute total, and the change to availability is the difference to the total we
 * already have. A concurrent reservation bumps the row version, so the save fails with an optimistic-lock error and
 * the binder redelivers the event against fresh data.
 */
@Service
@Transactional
public class StockSyncService {

    private static final Logger LOG = LoggerFactory.getLogger(StockSyncService.class);

    private final TicketStockRepository ticketStockRepository;

    public StockSyncService(TicketStockRepository ticketStockRepository) {
        this.ticketStockRepository = ticketStockRepository;
    }

    public void apply(TicketTypeEvent event) {
        ticketStockRepository
            .findByTicketTypeId(event.ticketTypeId())
            .ifPresentOrElse(
                stock -> resize(stock, event.totalQuantity()),
                () -> {
                    LOG.info("Creating stock for ticket type {} with {} seats", event.ticketTypeId(), event.totalQuantity());
                    ticketStockRepository.save(
                        new TicketStock()
                            .ticketTypeId(event.ticketTypeId())
                            .eventId(event.eventId())
                            .totalQuantity(event.totalQuantity())
                            .availableQuantity(event.totalQuantity())
                    );
                }
            );
    }

    private void resize(TicketStock stock, int newTotal) {
        int delta = newTotal - stock.getTotalQuantity();
        if (delta == 0) {
            return;
        }
        int sold = stock.getTotalQuantity() - stock.getAvailableQuantity();
        if (newTotal < sold) {
            // Can't un-sell seats: shrink to what is already taken and stop selling
            LOG.warn("Ticket type {} reduced to {} seats but {} are already held or sold", stock.getTicketTypeId(), newTotal, sold);
            stock.setTotalQuantity(sold);
            stock.setAvailableQuantity(0);
        } else {
            stock.setTotalQuantity(newTotal);
            stock.setAvailableQuantity(stock.getAvailableQuantity() + delta);
        }
        LOG.info("Resized stock of ticket type {} to {} seats", stock.getTicketTypeId(), stock.getTotalQuantity());
    }
}
