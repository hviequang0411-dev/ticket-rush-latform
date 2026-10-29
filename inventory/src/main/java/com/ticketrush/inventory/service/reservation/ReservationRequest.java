package com.ticketrush.inventory.service.reservation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Seats an order wants to hold. {@code orderId} doubles as the idempotency key: repeating the same request
 * returns the existing holds instead of taking more seats.
 */
public record ReservationRequest(@NotNull Long orderId, @NotEmpty @Size(max = 20) List<@Valid @NotNull Item> items) {
    public record Item(@NotNull Long ticketTypeId, @Min(1) @Max(10) int quantity) {}
}
