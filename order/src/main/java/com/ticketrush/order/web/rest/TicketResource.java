package com.ticketrush.order.web.rest;

import com.ticketrush.order.security.SecurityUtils;
import com.ticketrush.order.service.ticket.CheckInResult;
import com.ticketrush.order.service.ticket.TicketService;
import com.ticketrush.order.service.ticket.TicketView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

/**
 * E-tickets for their holders, and the gate scanner for staff ({@code /api/admin/**} is admins only).
 */
@RestController
@RequestMapping("/api")
public class TicketResource {

    private final TicketService ticketService;

    public TicketResource(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * {@code GET /api/my-tickets} : the current user's tickets, newest first.
     */
    @GetMapping("/my-tickets")
    public List<TicketView> myTickets() {
        return ticketService.findMine(currentUser());
    }

    /**
     * {@code POST /api/admin/check-in} : scan a QR code at the gate. Always 200; the outcome says whether to admit.
     */
    @PostMapping("/admin/check-in")
    public CheckInResult checkIn(@Valid @RequestBody Scan scan) {
        return ticketService.checkIn(scan.qrPayload(), currentUser());
    }

    public record Scan(@NotBlank @Size(max = 200) String qrPayload) {}

    private static String currentUser() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new AccessDeniedException("Not signed in"));
    }
}
