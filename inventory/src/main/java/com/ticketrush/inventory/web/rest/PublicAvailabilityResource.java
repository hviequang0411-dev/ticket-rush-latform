package com.ticketrush.inventory.web.rest;

import com.ticketrush.inventory.repository.TicketStockRepository;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Seats left per ticket type, for anonymous visitors (permitAll, read-only). Holds and orders stay private.
 */
@RestController
@RequestMapping("/api/public/availability")
@Validated
public class PublicAvailabilityResource {

    private final TicketStockRepository ticketStockRepository;

    public PublicAvailabilityResource(TicketStockRepository ticketStockRepository) {
        this.ticketStockRepository = ticketStockRepository;
    }

    /**
     * {@code GET /api/public/availability?ticketTypeIds=1,2} : unknown ids are simply absent from the answer.
     */
    @GetMapping("")
    @Transactional(readOnly = true)
    public List<Availability> availability(@RequestParam @NotEmpty @Size(max = 100) List<Long> ticketTypeIds) {
        return ticketStockRepository
            .findAllByTicketTypeIdIn(ticketTypeIds)
            .stream()
            .map(stock -> new Availability(stock.getTicketTypeId(), stock.getTotalQuantity(), stock.getAvailableQuantity()))
            .toList();
    }

    public record Availability(Long ticketTypeId, Integer totalQuantity, Integer availableQuantity) {}
}
