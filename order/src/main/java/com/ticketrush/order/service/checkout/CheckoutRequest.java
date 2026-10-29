package com.ticketrush.order.service.checkout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * What the buyer wants. Prices are not part of the request: they come from catalog.
 */
public record CheckoutRequest(@NotEmpty @Size(max = 10) List<@Valid @NotNull Item> items) {
    public record Item(@NotNull Long ticketTypeId, @Min(1) @Max(10) int quantity) {}
}
