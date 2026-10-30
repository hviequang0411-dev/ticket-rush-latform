package com.ticketrush.inventory.web.rest;

import com.ticketrush.inventory.domain.TicketStock;
import com.ticketrush.inventory.domain.enumeration.HoldStatus;
import com.ticketrush.inventory.repository.StockHoldRepository;
import com.ticketrush.inventory.repository.TicketStockRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Where every seat is, for organisers ({@code /api/admin/**} is admins only). For each ticket type
 * {@code available + held + sold == total}, which the stock constraints and the hold lifecycle keep true.
 */
@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminStockResource {

    private final TicketStockRepository ticketStockRepository;

    private final StockHoldRepository stockHoldRepository;

    public AdminStockResource(TicketStockRepository ticketStockRepository, StockHoldRepository stockHoldRepository) {
        this.ticketStockRepository = ticketStockRepository;
        this.stockHoldRepository = stockHoldRepository;
    }

    /**
     * {@code GET /api/admin/stock?ticketTypeIds=1,2} : seats sold, held and left per ticket type.
     */
    @GetMapping("/stock")
    @Transactional(readOnly = true)
    public List<StockLevel> stock(@RequestParam @NotEmpty @Size(max = 500) List<Long> ticketTypeIds) {
        Map<Long, Map<HoldStatus, Long>> seats = new HashMap<>();
        stockHoldRepository
            .sumSeats(ticketTypeIds, EnumSet.of(HoldStatus.HELD, HoldStatus.CONFIRMED))
            .forEach(row -> seats.computeIfAbsent(row.getTicketTypeId(), id -> new HashMap<>()).put(row.getStatus(), row.getQuantity()));
        return ticketStockRepository
            .findAllByTicketTypeIdIn(ticketTypeIds)
            .stream()
            .map(stock -> StockLevel.of(stock, seats.getOrDefault(stock.getTicketTypeId(), Map.of())))
            .toList();
    }

    /**
     * {@code GET /api/admin/holds/expiring?limit=20} : seats currently held, the ones about to go back on sale first.
     */
    @GetMapping("/holds/expiring")
    @Transactional(readOnly = true)
    public List<ActiveHold> expiringHolds(@RequestParam(defaultValue = "20") @Min(1) @Max(200) int limit) {
        return stockHoldRepository
            .findAllByStatusOrderByExpiresAtAsc(HoldStatus.HELD, PageRequest.of(0, limit))
            .stream()
            .map(hold ->
                new ActiveHold(
                    hold.getId(),
                    hold.getOrderId(),
                    hold.getStock().getTicketTypeId(),
                    hold.getStock().getEventId(),
                    hold.getQuantity(),
                    hold.getExpiresAt()
                )
            )
            .toList();
    }

    public record StockLevel(Long ticketTypeId, Long eventId, int totalQuantity, int availableQuantity, long heldQuantity, long soldQuantity) {
        static StockLevel of(TicketStock stock, Map<HoldStatus, Long> seats) {
            return new StockLevel(
                stock.getTicketTypeId(),
                stock.getEventId(),
                stock.getTotalQuantity(),
                stock.getAvailableQuantity(),
                seats.getOrDefault(HoldStatus.HELD, 0L),
                seats.getOrDefault(HoldStatus.CONFIRMED, 0L)
            );
        }
    }

    public record ActiveHold(Long holdId, Long orderId, Long ticketTypeId, Long eventId, int quantity, Instant expiresAt) {}
}
