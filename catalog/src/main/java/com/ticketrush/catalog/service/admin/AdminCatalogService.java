package com.ticketrush.catalog.service.admin;

import com.ticketrush.catalog.domain.Event;
import com.ticketrush.catalog.domain.TicketType;
import com.ticketrush.catalog.domain.Venue;
import com.ticketrush.catalog.domain.enumeration.EventStatus;
import com.ticketrush.catalog.repository.EventRepository;
import com.ticketrush.catalog.repository.TicketTypeRepository;
import com.ticketrush.catalog.repository.VenueRepository;
import com.ticketrush.catalog.service.event.TicketTypeChanged;
import com.ticketrush.catalog.service.event.TicketTypeChanges;
import com.ticketrush.catalog.service.publiccatalog.PublicEvent;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * The organiser's view of the catalog: any status, and an event saved together with its venue and ticket types in
 * one transaction. Ticket type changes reach inventory through the outbox, like every other catalog write.
 */
@Service
@Transactional
public class AdminCatalogService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminCatalogService.class);

    /** Allowed moves; CANCELLED and ENDED are final. */
    private static final Map<EventStatus, Set<EventStatus>> NEXT = Map.of(
        EventStatus.DRAFT,
        EnumSet.of(EventStatus.PUBLISHED, EventStatus.CANCELLED),
        EventStatus.PUBLISHED,
        EnumSet.of(EventStatus.CANCELLED, EventStatus.ENDED),
        EventStatus.CANCELLED,
        EnumSet.noneOf(EventStatus.class),
        EventStatus.ENDED,
        EnumSet.noneOf(EventStatus.class)
    );

    private final EventRepository eventRepository;

    private final VenueRepository venueRepository;

    private final TicketTypeRepository ticketTypeRepository;

    private final TicketTypeChanges ticketTypeChanges;

    public AdminCatalogService(
        EventRepository eventRepository,
        VenueRepository venueRepository,
        TicketTypeRepository ticketTypeRepository,
        TicketTypeChanges ticketTypeChanges
    ) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.ticketTypeRepository = ticketTypeRepository;
        this.ticketTypeChanges = ticketTypeChanges;
    }

    @Transactional(readOnly = true)
    public Page<PublicEvent> search(String query, Pageable pageable) {
        String pattern = "%" + (query == null ? "" : query.trim().toLowerCase(Locale.ROOT)) + "%";
        Page<Event> page = eventRepository.searchWithVenue(pattern, pageable);
        Map<Long, List<TicketType>> types = ticketTypesOf(page.getContent());
        return page.map(event -> PublicEvent.of(event, types.getOrDefault(event.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public Optional<PublicEvent> findOne(Long id) {
        return eventRepository.findOneWithEagerRelationships(id).map(this::view);
    }

    /** New events start as DRAFT unless {@code publish} is set. */
    public PublicEvent create(EventForm form, boolean publish) {
        if (eventRepository.existsBySlug(form.slug())) {
            throw slugTaken(form.slug());
        }
        Event event = eventRepository.save(
            apply(form, new Event()).status(publish ? EventStatus.PUBLISHED : EventStatus.DRAFT).venue(venueFor(form))
        );
        for (EventForm.TicketTypeForm typeForm : form.ticketTypes()) {
            if (typeForm.id() != null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A new event has no existing ticket types");
            }
            addTicketType(event, typeForm);
        }
        LOG.info("Created event {} ({})", event.getSlug(), event.getStatus());
        return view(event);
    }

    public PublicEvent update(Long id, EventForm form) {
        Event event = eventRepository.findOneWithEagerRelationships(id).orElseThrow(() -> notFound(id));
        if (eventRepository.existsBySlugAndIdNot(form.slug(), id)) {
            throw slugTaken(form.slug());
        }
        if (EnumSet.of(EventStatus.CANCELLED, EventStatus.ENDED).contains(event.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event " + id + " is " + event.getStatus() + " and can no longer change");
        }
        apply(form, event).venue(venueFor(form));
        Map<Long, TicketType> existing = ticketTypeRepository
            .findAllByEventIds(List.of(id))
            .stream()
            .collect(Collectors.toMap(TicketType::getId, Function.identity()));
        for (EventForm.TicketTypeForm typeForm : form.ticketTypes()) {
            if (typeForm.id() == null) {
                addTicketType(event, typeForm);
                continue;
            }
            TicketType type = existing.get(typeForm.id());
            if (type == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket type " + typeForm.id() + " is not part of event " + id);
            }
            boolean resized = !Objects.equals(type.getTotalQuantity(), typeForm.totalQuantity());
            type
                .name(typeForm.name())
                .price(typeForm.price())
                .totalQuantity(typeForm.totalQuantity())
                .saleStart(typeForm.saleStart())
                .saleEnd(typeForm.saleEnd());
            if (resized) {
                // Inventory never goes below what is already held or sold
                ticketTypeChanges.raise(TicketTypeChanged.Type.UPDATED, ticketTypeRepository.save(type));
            }
        }
        return view(eventRepository.save(event));
    }

    public PublicEvent changeStatus(Long id, EventStatus to) {
        Event event = eventRepository.findOneWithEagerRelationships(id).orElseThrow(() -> notFound(id));
        if (event.getStatus() == to) {
            return view(event);
        }
        if (!NEXT.get(event.getStatus()).contains(to)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Event " + id + " cannot go from " + event.getStatus() + " to " + to);
        }
        LOG.info("Event {}: {} -> {}", event.getSlug(), event.getStatus(), to);
        return view(eventRepository.save(event.status(to)));
    }

    private Event apply(EventForm form, Event event) {
        return event
            .name(form.name().trim())
            .slug(form.slug())
            .description(form.description())
            .startTime(form.startTime())
            .endTime(form.endTime())
            .posterUrl(form.posterUrl())
            .waitingRoom(Boolean.TRUE.equals(form.waitingRoom()));
    }

    /** Venues are shared between events: the same name in the same city is the same place. */
    private Venue venueFor(EventForm form) {
        EventForm.VenueForm v = form.venue();
        int seats = form.ticketTypes().stream().mapToInt(EventForm.TicketTypeForm::totalQuantity).sum();
        return venueRepository
            .findFirstByNameIgnoreCaseAndCityIgnoreCase(v.name().trim(), v.city().trim())
            .orElseGet(() ->
                venueRepository.save(
                    new Venue()
                        .name(v.name().trim())
                        .city(v.city().trim())
                        .address(v.address() == null || v.address().isBlank() ? v.city().trim() : v.address().trim())
                        .capacity(v.capacity() != null ? v.capacity() : Math.max(1, seats))
                )
            );
    }

    private void addTicketType(Event event, EventForm.TicketTypeForm form) {
        TicketType type = ticketTypeRepository.save(
            new TicketType()
                .name(form.name().trim())
                .price(form.price())
                .currency("VND")
                .totalQuantity(form.totalQuantity())
                .saleStart(form.saleStart())
                .saleEnd(form.saleEnd())
                .event(event)
        );
        ticketTypeChanges.raise(TicketTypeChanged.Type.CREATED, type);
    }

    private PublicEvent view(Event event) {
        return PublicEvent.of(event, ticketTypesOf(List.of(event)).getOrDefault(event.getId(), List.of()));
    }

    private Map<Long, List<TicketType>> ticketTypesOf(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }
        return ticketTypeRepository
            .findAllByEventIds(events.stream().map(Event::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(type -> type.getEvent().getId()));
    }

    private static ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Event " + id + " not found");
    }

    private static ResponseStatusException slugTaken(String slug) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Slug " + slug + " is already used by another event");
    }
}
