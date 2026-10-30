package com.ticketrush.catalog.service.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * An event with its venue and ticket types, as the organiser fills it in. On update, ticket types with an id are
 * changed and those without one are added; ticket types are never deleted (orders point at them).
 */
public record EventForm(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 220) @Pattern(regexp = "[a-z0-9]+(-[a-z0-9]+)*") String slug,
    @Size(max = 10000) String description,
    @NotNull Instant startTime,
    Instant endTime,
    @Size(max = 500) String posterUrl,
    Boolean waitingRoom,
    @NotNull @Valid VenueForm venue,
    @NotEmpty @Size(max = 20) List<@Valid TicketTypeForm> ticketTypes
) {
    public record VenueForm(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String address,
        @NotBlank @Size(max = 100) String city,
        @Min(1) Integer capacity
    ) {}

    public record TicketTypeForm(
        Long id,
        @NotBlank @Size(max = 100) String name,
        @NotNull @DecimalMin("0") BigDecimal price,
        @NotNull @Min(1) @Max(1_000_000) Integer totalQuantity,
        Instant saleStart,
        Instant saleEnd
    ) {}
}
