package com.ticketrush.inventory.web.rest;

import com.ticketrush.inventory.service.reservation.Reservation;
import com.ticketrush.inventory.service.reservation.ReservationRequest;
import com.ticketrush.inventory.service.reservation.ReservationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Seat holds for the order service. Not exposed to end users: callers need {@code ROLE_SERVICE}
 * (see SecurityConfiguration).
 * <ul>
 *   <li>409 when seats are sold out, or the holds can no longer be confirmed</li>
 *   <li>400 when a ticket type has no stock</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/internal/reservations")
public class InternalReservationResource {

    private static final Logger LOG = LoggerFactory.getLogger(InternalReservationResource.class);

    private final ReservationService reservationService;

    public InternalReservationResource(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    /**
     * {@code POST /api/internal/reservations} : hold seats for an order, all or nothing. Repeating the call for the
     * same order returns the existing holds.
     */
    @PostMapping("")
    public ResponseEntity<Reservation> reserve(@Valid @RequestBody ReservationRequest request) {
        LOG.debug("REST request to reserve seats : {}", request);
        return ResponseEntity.ok(reservationService.reserve(request));
    }

    /**
     * {@code POST /api/internal/reservations/{orderId}/confirm} : mark the held seats as sold.
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<Reservation> confirm(@PathVariable Long orderId) {
        LOG.debug("REST request to confirm holds of order : {}", orderId);
        return ResponseEntity.ok(reservationService.confirm(orderId));
    }

    /**
     * {@code POST /api/internal/reservations/{orderId}/release} : put the held seats back on sale.
     */
    @PostMapping("/{orderId}/release")
    public ResponseEntity<Reservation> release(@PathVariable Long orderId) {
        LOG.debug("REST request to release holds of order : {}", orderId);
        return ResponseEntity.ok(reservationService.release(orderId));
    }
}
