package com.ticketrush.catalog.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import com.ticketrush.catalog.IntegrationTest;
import com.ticketrush.catalog.domain.OutboxEvent;
import com.ticketrush.catalog.repository.EventRepository;
import com.ticketrush.catalog.repository.OutboxEventRepository;
import com.ticketrush.catalog.repository.TicketTypeRepository;
import com.ticketrush.catalog.repository.VenueRepository;
import com.ticketrush.catalog.security.AuthoritiesConstants;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class AdminCatalogResourceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @AfterEach
    void cleanup() {
        outboxEventRepository.deleteAll();
        ticketTypeRepository.deleteAll();
        eventRepository.deleteAll();
        venueRepository.deleteAll();
    }

    @Test
    void createsEventVenueAndTicketTypesInOneGoAndTellsInventory() throws Exception {
        String slug = slug();
        String body = mockMvc
            .perform(post("/api/admin/events?publish=true").contentType(MediaType.APPLICATION_JSON).content(form(slug, "", 100, "")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("PUBLISHED"))
            .andExpect(jsonPath("$.venue.city").value("Hà Nội"))
            .andExpect(jsonPath("$.ticketTypes.length()").value(2))
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(ticketTypeEvents()).containsExactlyInAnyOrder("CREATED", "CREATED");
        // Shows up for buyers straight away
        mockMvc.perform(get("/api/public/events/" + slug)).andExpect(status().isOk());
        assertThat((Integer) JsonPath.read(body, "$.venue.capacity")).isEqualTo(150);
    }

    @Test
    void draftsStayHiddenUntilPublished() throws Exception {
        String slug = slug();
        long id = create(slug, false);
        mockMvc.perform(get("/api/public/events/" + slug)).andExpect(status().isNotFound());

        mockMvc
            .perform(post("/api/admin/events/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"));
        mockMvc.perform(get("/api/public/events/" + slug)).andExpect(status().isOk());
    }

    @Test
    void waitingRoomIsSwitchedPerEventAndShownToBuyers() throws Exception {
        String slug = slug();
        String body = form(slug, "", 100, "").replace("\"startTime\"", "\"waitingRoom\":true,\"startTime\"");
        mockMvc
            .perform(post("/api/admin/events?publish=true").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.waitingRoom").value(true));
        mockMvc.perform(get("/api/public/events/" + slug)).andExpect(jsonPath("$.waitingRoom").value(true));
    }

    @Test
    void cancelledEventsAreFinal() throws Exception {
        long id = create(slug(), true);
        mockMvc
            .perform(post("/api/admin/events/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
            .andExpect(status().isOk());
        mockMvc
            .perform(post("/api/admin/events/" + id + "/status").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PUBLISHED\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    void slugsAreUnique() throws Exception {
        String slug = slug();
        create(slug, false);
        mockMvc
            .perform(post("/api/admin/events").contentType(MediaType.APPLICATION_JSON).content(form(slug, "", 100, "")))
            .andExpect(status().isConflict());
    }

    @Test
    void updateResizesExistingTicketTypesAndAddsNewOnes() throws Exception {
        String slug = slug();
        long id = create(slug, true);
        String event = mockMvc.perform(get("/api/admin/events/" + id)).andReturn().getResponse().getContentAsString();
        List<Integer> typeIds = JsonPath.read(event, "$.ticketTypes[*].id");
        outboxEventRepository.deleteAll();

        String extra = ",{\"name\":\"Early bird\",\"price\":300000,\"totalQuantity\":20}";
        String withIds = form(slug, "\"id\":" + typeIds.get(0) + ",", 120, extra).replaceFirst(
            "\\{\"name\":\"Thường\"",
            "{\"id\":" + typeIds.get(1) + ",\"name\":\"Thường\""
        );
        mockMvc
            .perform(put("/api/admin/events/" + id).contentType(MediaType.APPLICATION_JSON).content(withIds))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ticketTypes.length()").value(3));

        // VIP resized, standard unchanged, early bird new
        assertThat(ticketTypeEvents()).containsExactlyInAnyOrder("UPDATED", "CREATED");
    }

    @Test
    void searchFindsAnyStatusByNameOrCity() throws Exception {
        create(slug(), false);
        mockMvc
            .perform(get("/api/admin/events?q=hà nội"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void buyersCannotManageEvents() throws Exception {
        mockMvc.perform(get("/api/admin/events")).andExpect(status().isForbidden());
        mockMvc
            .perform(post("/api/admin/events").contentType(MediaType.APPLICATION_JSON).content(form(slug(), "", 100, "")))
            .andExpect(status().isForbidden());
    }

    private long create(String slug, boolean publish) throws Exception {
        String body = mockMvc
            .perform(post("/api/admin/events?publish=" + publish).contentType(MediaType.APPLICATION_JSON).content(form(slug, "", 100, "")))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private List<String> ticketTypeEvents() {
        return outboxEventRepository.findAll().stream().filter(e -> e.getAggregateType().equals("TicketType")).map(OutboxEvent::getEventType).toList();
    }

    private static String slug() {
        return "admin-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String form(String slug, String vipId, int vipSeats, String extraTypes) {
        return (
            "{\"name\":\"Đêm nhạc\",\"slug\":\"" +
            slug +
            "\",\"startTime\":\"2027-01-10T13:00:00Z\"," +
            "\"venue\":{\"name\":\"Nhà hát Lớn\",\"city\":\"Hà Nội\"}," +
            "\"ticketTypes\":[{" +
            vipId +
            "\"name\":\"VIP\",\"price\":2000000,\"totalQuantity\":" +
            vipSeats +
            "},{\"name\":\"Thường\",\"price\":500000,\"totalQuantity\":50}" +
            extraTypes +
            "]}"
        );
    }
}
