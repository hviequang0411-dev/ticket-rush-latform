package com.ticketrush.catalog.service.publiccatalog;

import com.ticketrush.catalog.domain.Event;
import com.ticketrush.catalog.domain.TicketType;
import com.ticketrush.catalog.domain.enumeration.EventStatus;
import com.ticketrush.catalog.repository.EventRepository;
import com.ticketrush.catalog.repository.TicketTypeRepository;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The catalog as buyers see it, without signing in. Drafts are never visible.
 */
@Service
@Transactional(readOnly = true)
public class PublicCatalogService {

    /** On sale: the listing and event pages. */
    private static final Set<EventStatus> LISTED = EnumSet.of(EventStatus.PUBLISHED);

    /** Still looked up by id from past orders. */
    private static final Set<EventStatus> VISIBLE = EnumSet.of(EventStatus.PUBLISHED, EventStatus.ENDED, EventStatus.CANCELLED);

    private final EventRepository eventRepository;

    private final TicketTypeRepository ticketTypeRepository;

    public PublicCatalogService(EventRepository eventRepository, TicketTypeRepository ticketTypeRepository) {
        this.eventRepository = eventRepository;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    public List<PublicEvent> listOnSale(int page, int size) {
        return withTicketTypes(eventRepository.findAllWithVenueByStatusIn(LISTED, PageRequest.of(page, size)));
    }

    public Optional<PublicEvent> findBySlug(String slug) {
        return eventRepository
            .findOneWithVenueBySlug(slug)
            .filter(event -> LISTED.contains(event.getStatus()))
            .map(event -> withTicketTypes(List.of(event)).get(0));
    }

    public Optional<PublicEvent> findById(Long id) {
        return eventRepository
            .findOneWithEagerRelationships(id)
            .filter(event -> VISIBLE.contains(event.getStatus()))
            .map(event -> withTicketTypes(List.of(event)).get(0));
    }

    private List<PublicEvent> withTicketTypes(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        Map<Long, List<TicketType>> byEvent = ticketTypeRepository
            .findAllByEventIds(events.stream().map(Event::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(type -> type.getEvent().getId()));
        return events.stream().map(event -> PublicEvent.of(event, byEvent.getOrDefault(event.getId(), List.of()))).toList();
    }
}
