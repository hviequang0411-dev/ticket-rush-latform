package com.ticketrush.catalog.service.publiccatalog;

import com.ticketrush.catalog.domain.Event;
import com.ticketrush.catalog.domain.TicketType;
import com.ticketrush.catalog.domain.Venue;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * What anyone may see of an event before signing in: the event, its venue and its ticket types with prices.
 * Availability is not here, it belongs to inventory.
 */
public record PublicEvent(
    Long id,
    String name,
    String slug,
    String description,
    Instant startTime,
    Instant endTime,
    String status,
    String posterUrl,
    boolean waitingRoom,
    PublicVenue venue,
    List<PublicTicketType> ticketTypes
) {
    public record PublicVenue(Long id, String name, String address, String city, Integer capacity) {
        static PublicVenue of(Venue venue) {
            return new PublicVenue(venue.getId(), venue.getName(), venue.getAddress(), venue.getCity(), venue.getCapacity());
        }
    }

    public record PublicTicketType(
        Long id,
        String name,
        BigDecimal price,
        String currency,
        Integer totalQuantity,
        Instant saleStart,
        Instant saleEnd
    ) {
        static PublicTicketType of(TicketType type) {
            return new PublicTicketType(
                type.getId(),
                type.getName(),
                type.getPrice(),
                type.getCurrency(),
                type.getTotalQuantity(),
                type.getSaleStart(),
                type.getSaleEnd()
            );
        }
    }

    public static PublicEvent of(Event event, List<TicketType> ticketTypes) {
        return new PublicEvent(
            event.getId(),
            event.getName(),
            event.getSlug(),
            event.getDescription(),
            event.getStartTime(),
            event.getEndTime(),
            event.getStatus().name(),
            event.getPosterUrl(),
            Boolean.TRUE.equals(event.getWaitingRoom()),
            PublicVenue.of(event.getVenue()),
            ticketTypes.stream().map(PublicTicketType::of).toList()
        );
    }
}
