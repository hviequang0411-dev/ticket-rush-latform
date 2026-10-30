package com.ticketrush.catalog.web.rest;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ticketrush.catalog.IntegrationTest;
import com.ticketrush.catalog.domain.Event;
import com.ticketrush.catalog.domain.TicketType;
import com.ticketrush.catalog.domain.Venue;
import com.ticketrush.catalog.domain.enumeration.EventStatus;
import com.ticketrush.catalog.repository.EventRepository;
import com.ticketrush.catalog.repository.TicketTypeRepository;
import com.ticketrush.catalog.repository.VenueRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Runs without any authenticated user: this is what an anonymous visitor gets.
 */
@IntegrationTest
@AutoConfigureMockMvc
class PublicCatalogResourceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    private Event published;

    private Event draft;

    private Event ended;

    @BeforeEach
    void createEvents() {
        Venue venue = venueRepository.save(new Venue().name("My Dinh").address("Le Duc Tho").city("Ha Noi").capacity(40000));
        published = event(venue, EventStatus.PUBLISHED, "2031-06-01T12:00:00Z");
        draft = event(venue, EventStatus.DRAFT, "2031-05-01T12:00:00Z");
        ended = event(venue, EventStatus.ENDED, "2020-01-01T12:00:00Z");
        ticketType(published, "VIP", "2500000");
        ticketType(published, "GA", "800000");
    }

    @AfterEach
    void cleanup() {
        ticketTypeRepository.deleteAll();
        eventRepository.deleteAll();
        venueRepository.deleteAll();
    }

    @Test
    void listingShowsOnlyEventsOnSaleWithVenueAndPrices() throws Exception {
        mockMvc
            .perform(get("/api/public/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].slug").value(hasItem(published.getSlug())))
            .andExpect(jsonPath("$[*].slug").value(not(hasItem(draft.getSlug()))))
            .andExpect(jsonPath("$[*].slug").value(not(hasItem(ended.getSlug()))))
            .andExpect(jsonPath("$[?(@.slug == '" + published.getSlug() + "')].venue.city").value(hasItem("Ha Noi")))
            .andExpect(jsonPath("$[?(@.slug == '" + published.getSlug() + "')].ticketTypes[0].name").value(hasItem("VIP")));
    }

    @Test
    void eventPageBySlug() throws Exception {
        mockMvc
            .perform(get("/api/public/events/{slug}", published.getSlug()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(published.getId()))
            .andExpect(jsonPath("$.ticketTypes.length()").value(2))
            .andExpect(jsonPath("$.ticketTypes[1].price").value(800000));
        mockMvc.perform(get("/api/public/events/{slug}", draft.getSlug())).andExpect(status().isNotFound());
    }

    @Test
    void pastOrdersCanStillLookUpEndedEventsButNeverDrafts() throws Exception {
        mockMvc.perform(get("/api/public/events/by-id/{id}", ended.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENDED"));
        mockMvc.perform(get("/api/public/events/by-id/{id}", draft.getId())).andExpect(status().isNotFound());
    }

    @Test
    void anonymousVisitorsStillCannotChangeTheCatalog() throws Exception {
        mockMvc.perform(post("/api/events").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
    }

    private Event event(Venue venue, EventStatus status, String start) {
        return eventRepository.save(
            new Event()
                .name("Event " + status)
                .slug("public-" + status.name().toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8))
                .startTime(Instant.parse(start))
                .status(status)
                .venue(venue)
        );
    }

    private void ticketType(Event event, String name, String price) {
        ticketTypeRepository.save(new TicketType().name(name).price(new BigDecimal(price)).currency("VND").totalQuantity(100).event(event));
    }
}
