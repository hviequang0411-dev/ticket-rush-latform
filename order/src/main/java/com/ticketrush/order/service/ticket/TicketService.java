package com.ticketrush.order.service.ticket;

import com.ticketrush.order.domain.Order;
import com.ticketrush.order.domain.OrderItem;
import com.ticketrush.order.domain.Ticket;
import com.ticketrush.order.domain.enumeration.TicketStatus;
import com.ticketrush.order.repository.TicketRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues e-tickets for confirmed orders and admits them at the gate.
 */
@Service
public class TicketService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;

    private final TicketCodes codes;

    private final Clock clock;

    private final MeterRegistry meterRegistry;

    public TicketService(TicketRepository ticketRepository, TicketCodes codes, Clock clock, MeterRegistry meterRegistry) {
        this.ticketRepository = ticketRepository;
        this.codes = codes;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    /**
     * One ticket per seat. Runs inside the saga's transaction that confirms the order, so an order is never CONFIRMED
     * without its tickets. Idempotent: the confirmation happens once per order, and the unique seat constraint backs
     * that up.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void issue(Order order) {
        if (ticketRepository.existsByOrderId(order.getId())) {
            return;
        }
        Instant now = clock.instant();
        List<OrderItem> items = order.getItemses().stream().sorted(Comparator.comparing(OrderItem::getTicketTypeId)).toList();
        for (OrderItem item : items) {
            for (int seat = 1; seat <= item.getQuantity(); seat++) {
                ticketRepository.save(new Ticket(codes.newCode(), order, item, seat, now));
            }
        }
        LOG.info("Issued tickets for order {}", order.getCode());
    }

    @Transactional(readOnly = true)
    public List<TicketView> findMine(String userLogin) {
        return ticketRepository.findAllByUserLoginOrderByIssuedAtDescIdAsc(userLogin).stream().map(t -> TicketView.of(t, codes)).toList();
    }

    @Transactional(readOnly = true)
    public List<TicketView> findByOrder(Long orderId) {
        return ticketRepository.findAllByOrderIdOrderByIdAsc(orderId).stream().map(t -> TicketView.of(t, codes)).toList();
    }

    /**
     * Scans a QR payload at the gate. Safe to call from many gates at once: exactly one scan of a ticket admits it.
     */
    @Transactional
    public CheckInResult checkIn(String qrPayload, String staffLogin) {
        Optional<String> code = codes.verify(qrPayload == null ? null : qrPayload.trim());
        if (code.isEmpty()) {
            return outcome(CheckInResult.Outcome.INVALID, null);
        }
        if (ticketRepository.checkIn(code.orElseThrow(), clock.instant(), staffLogin) == 1) {
            return outcome(CheckInResult.Outcome.ADMITTED, view(code.orElseThrow()));
        }
        Optional<TicketView> ticket = ticketRepository.findOneByCode(code.orElseThrow()).map(t -> TicketView.of(t, codes));
        if (ticket.isEmpty()) {
            return outcome(CheckInResult.Outcome.INVALID, null);
        }
        CheckInResult.Outcome outcome = ticket.orElseThrow().status() == TicketStatus.CANCELLED
            ? CheckInResult.Outcome.CANCELLED
            : CheckInResult.Outcome.ALREADY_USED;
        return outcome(outcome, ticket.orElseThrow());
    }

    private TicketView view(String code) {
        return ticketRepository.findOneByCode(code).map(t -> TicketView.of(t, codes)).orElseThrow();
    }

    private CheckInResult outcome(CheckInResult.Outcome outcome, TicketView ticket) {
        meterRegistry.counter("ticketrush.checkins", "outcome", outcome.name()).increment();
        return new CheckInResult(outcome, ticket);
    }
}
