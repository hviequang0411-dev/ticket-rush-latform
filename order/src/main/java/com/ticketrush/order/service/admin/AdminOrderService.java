package com.ticketrush.order.service.admin;

import com.ticketrush.order.domain.enumeration.OrderStatus;
import com.ticketrush.order.repository.OrderRepository;
import com.ticketrush.order.service.ticket.TicketService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orders across all buyers, and the sales figures behind the organiser's dashboard.
 */
@Service
@Transactional(readOnly = true)
public class AdminOrderService {

    private static final Set<OrderStatus> HOLDING_SEATS = EnumSet.of(OrderStatus.RESERVED, OrderStatus.AWAITING_PAYMENT);

    private static final Set<OrderStatus> FINISHED = EnumSet.of(
        OrderStatus.CONFIRMED,
        OrderStatus.CANCELLED,
        OrderStatus.EXPIRED,
        OrderStatus.FAILED,
        OrderStatus.REFUNDED
    );

    private final OrderRepository orderRepository;

    private final TicketService ticketService;

    private final Clock clock;

    public AdminOrderService(OrderRepository orderRepository, TicketService ticketService, Clock clock) {
        this.orderRepository = orderRepository;
        this.ticketService = ticketService;
        this.clock = clock;
    }

    public Page<AdminOrder> search(String query, OrderStatus status, Pageable pageable) {
        String pattern = "%" + (query == null ? "" : query.trim().toLowerCase(Locale.ROOT)) + "%";
        return orderRepository.search(pattern, status, pageable).map(order -> AdminOrder.of(order, List.of()));
    }

    public Optional<AdminOrder> findOne(String code) {
        return orderRepository.findOneWithItemsByCode(code).map(order -> AdminOrder.of(order, ticketService.findByOrder(order.getId())));
    }

    public SalesStats stats(ZoneId zone, int days, int paceMinutes) {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, zone);
        Instant startOfToday = today.atStartOfDay(zone).toInstant();
        Instant startOfYesterday = today.minusDays(1).atStartOfDay(zone).toInstant();

        Map<LocalDate, SalesStats.DailyRevenue> revenue = new HashMap<>();
        orderRepository
            .dailyRevenue(today.minusDays(days - 1L).atStartOfDay(zone).toInstant(), zone.getId())
            .forEach(row -> revenue.put(row.getDay(), new SalesStats.DailyRevenue(row.getDay(), row.getRevenue(), row.getOrders())));
        // Every day present, quiet days as zero, so the chart has no gaps
        List<SalesStats.DailyRevenue> daily = today
            .minusDays(days - 1L)
            .datesUntil(today.plusDays(1))
            .map(date -> revenue.getOrDefault(date, new SalesStats.DailyRevenue(date, BigDecimal.ZERO, 0)))
            .toList();

        List<SalesStats.EventPace> pace = orderRepository
            .seatsConfirmedSince(now.minus(Duration.ofMinutes(paceMinutes)))
            .stream()
            .map(row -> new SalesStats.EventPace(row.getEventId(), row.getSeats()))
            .toList();

        return new SalesStats(
            zone.getId(),
            day(today, startOfToday, now, revenue),
            day(today.minusDays(1), startOfYesterday, startOfToday, revenue),
            orderRepository.countByStatusIn(HOLDING_SEATS),
            daily,
            paceMinutes,
            pace
        );
    }

    private SalesStats.Day day(LocalDate date, Instant from, Instant to, Map<LocalDate, SalesStats.DailyRevenue> revenue) {
        long placed = 0;
        long confirmed = 0;
        long finished = 0;
        for (OrderRepository.OrdersByStatus row : orderRepository.countByStatusPlacedBetween(from, to)) {
            placed += row.getOrders();
            if (row.getStatus() == OrderStatus.CONFIRMED) {
                confirmed += row.getOrders();
            }
            if (FINISHED.contains(row.getStatus())) {
                finished += row.getOrders();
            }
        }
        BigDecimal confirmedRevenue = Optional.ofNullable(revenue.get(date)).map(SalesStats.DailyRevenue::revenue).orElse(BigDecimal.ZERO);
        return new SalesStats.Day(date, confirmedRevenue, placed, confirmed, finished);
    }
}
