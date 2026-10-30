package com.ticketrush.inventory.service.reservation;

import com.ticketrush.inventory.broker.HoldExpiredEvent;
import com.ticketrush.inventory.config.ApplicationProperties;
import com.ticketrush.inventory.domain.StockHold;
import com.ticketrush.inventory.domain.TicketStock;
import com.ticketrush.inventory.domain.enumeration.HoldStatus;
import com.ticketrush.inventory.repository.StockHoldRepository;
import com.ticketrush.inventory.repository.TicketStockRepository;
import com.ticketrush.inventory.service.outbox.Outbox;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Holds, confirms and releases seats for orders.
 * <p>
 * Overselling is prevented by {@link TicketStockRepository#tryTake}: a single conditional UPDATE, so the
 * availability check and the decrement cannot be interleaved by another transaction. No application lock and no
 * read-then-write. See docs/adr/0003-oversell-prevention.md.
 */
@Service
@Transactional
public class ReservationService {

    private static final Logger LOG = LoggerFactory.getLogger(ReservationService.class);

    private final TicketStockRepository ticketStockRepository;

    private final StockHoldRepository stockHoldRepository;

    private final ApplicationProperties applicationProperties;

    private final Clock clock;

    private final MeterRegistry meterRegistry;

    private final Outbox outbox;

    public ReservationService(
        TicketStockRepository ticketStockRepository,
        StockHoldRepository stockHoldRepository,
        ApplicationProperties applicationProperties,
        Clock clock,
        Outbox outbox,
        MeterRegistry meterRegistry
    ) {
        this.ticketStockRepository = ticketStockRepository;
        this.stockHoldRepository = stockHoldRepository;
        this.applicationProperties = applicationProperties;
        this.clock = clock;
        this.outbox = outbox;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Holds all requested seats or none of them.
     *
     * @throws InsufficientStockException if any ticket type is short; seats already taken in this call roll back.
     * @throws UnknownTicketTypeException if a ticket type has no stock record.
     * @throws ReservationConflictException if the order already holds a different set of seats.
     */
    public Reservation reserve(ReservationRequest request) {
        LOG.debug("Request to reserve seats : {}", request);
        Map<Long, Integer> requested = request
            .items()
            .stream()
            .collect(Collectors.toMap(ReservationRequest.Item::ticketTypeId, ReservationRequest.Item::quantity, Integer::sum, TreeMap::new));

        List<StockHold> existing = stockHoldRepository.findAllByOrderId(request.orderId());
        if (!existing.isEmpty()) {
            count("replayed");
            return replay(request.orderId(), requested, existing);
        }

        List<TicketStock> stocks = new ArrayList<>(ticketStockRepository.findAllByTicketTypeIdIn(requested.keySet()));
        if (stocks.size() != requested.size()) {
            Set<Long> missing = new HashSet<>(requested.keySet());
            stocks.forEach(stock -> missing.remove(stock.getTicketTypeId()));
            count("unknown_ticket_type");
            throw new UnknownTicketTypeException(missing);
        }
        // Always lock rows in the same order, so two multi-item orders cannot deadlock each other
        stocks.sort(Comparator.comparing(TicketStock::getId));

        Instant now = clock.instant();
        Instant expiresAt = now.plus(applicationProperties.getReservation().getHoldTtl());
        List<StockHold> holds = new ArrayList<>();
        for (TicketStock stock : stocks) {
            int quantity = requested.get(stock.getTicketTypeId());
            if (ticketStockRepository.tryTake(stock.getId(), quantity) == 0) {
                count("sold_out");
                throw new InsufficientStockException(stock.getTicketTypeId(), quantity);
            }
            holds.add(
                new StockHold().orderId(request.orderId()).quantity(quantity).status(HoldStatus.HELD).expiresAt(expiresAt).createdAt(now).stock(stock)
            );
        }
        count("held");
        meterRegistry.counter("ticketrush.seats.held").increment(holds.stream().mapToInt(StockHold::getQuantity).sum());
        return Reservation.of(request.orderId(), stockHoldRepository.saveAll(holds));
    }

    /**
     * Turns the order's holds into sold seats. Idempotent once confirmed.
     *
     * @throws ReservationConflictException if the holds expired or were released.
     */
    public Reservation confirm(Long orderId) {
        LOG.debug("Request to confirm holds of order : {}", orderId);
        List<StockHold> holds = requireHolds(orderId);
        Instant now = clock.instant();
        for (StockHold hold : holds) {
            if (hold.getStatus() == HoldStatus.CONFIRMED) {
                continue;
            }
            if (hold.getStatus() != HoldStatus.HELD || !hold.getExpiresAt().isAfter(now)) {
                throw new ReservationConflictException("Holds of order " + orderId + " are no longer active");
            }
            hold.setStatus(HoldStatus.CONFIRMED);
        }
        return Reservation.of(orderId, holds);
    }

    /**
     * Puts the order's held seats back on sale. Idempotent; confirmed seats are left untouched.
     */
    public Reservation release(Long orderId) {
        LOG.debug("Request to release holds of order : {}", orderId);
        List<StockHold> holds = requireHolds(orderId);
        for (StockHold hold : holds) {
            if (hold.getStatus() == HoldStatus.HELD) {
                giveBack(hold);
                hold.setStatus(HoldStatus.RELEASED);
            }
        }
        return Reservation.of(orderId, holds);
    }

    /**
     * Releases one batch of holds whose time ran out.
     *
     * @return the released holds; fewer than the batch size means there is nothing left to expire.
     */
    public List<ExpiredHold> expireDueHolds() {
        Instant now = clock.instant();
        List<StockHold> due = stockHoldRepository.lockExpiredHolds(now, applicationProperties.getReservation().getExpiryBatchSize());
        List<ExpiredHold> expired = new ArrayList<>(due.size());
        for (StockHold hold : due) {
            giveBack(hold);
            hold.setStatus(HoldStatus.EXPIRED);
            expired.add(new ExpiredHold(hold.getOrderId(), hold.getStock().getTicketTypeId(), hold.getQuantity(), now));
        }
        // One event per order, written in the transaction that put the seats back on sale
        expired
            .stream()
            .map(ExpiredHold::orderId)
            .distinct()
            .forEach(orderId -> outbox.append(HoldExpiredEvent.TOPIC, "Order", orderId, "HOLD_EXPIRED", new HoldExpiredEvent(orderId, now)));
        if (!expired.isEmpty()) {
            LOG.info("Expired {} holds", expired.size());
        }
        return expired;
    }

    private Reservation replay(Long orderId, Map<Long, Integer> requested, List<StockHold> existing) {
        Map<Long, Integer> held = existing
            .stream()
            .collect(Collectors.toMap(hold -> hold.getStock().getTicketTypeId(), StockHold::getQuantity, Integer::sum, TreeMap::new));
        if (!held.equals(requested)) {
            throw new ReservationConflictException("Order " + orderId + " already holds different seats");
        }
        LOG.debug("Order {} already has holds, returning them", orderId);
        return Reservation.of(orderId, existing);
    }

    private void count(String outcome) {
        meterRegistry.counter("ticketrush.reservations", "outcome", outcome).increment();
    }

    private List<StockHold> requireHolds(Long orderId) {
        List<StockHold> holds = stockHoldRepository.findAllByOrderId(orderId);
        if (holds.isEmpty()) {
            throw new ReservationConflictException("Order " + orderId + " has no holds");
        }
        return holds;
    }

    private void giveBack(StockHold hold) {
        if (ticketStockRepository.giveBack(hold.getStock().getId(), hold.getQuantity()) == 0) {
            // Would push available above total: the stock was edited under us. Keep the hold state change anyway.
            LOG.error("Could not return {} seats of hold {} to stock {}", hold.getQuantity(), hold.getId(), hold.getStock().getId());
        }
    }
}
