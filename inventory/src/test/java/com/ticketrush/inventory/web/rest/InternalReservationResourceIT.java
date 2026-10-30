package com.ticketrush.inventory.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ticketrush.inventory.IntegrationTest;
import com.ticketrush.inventory.domain.TicketStock;
import com.ticketrush.inventory.repository.StockHoldRepository;
import com.ticketrush.inventory.repository.TicketStockRepository;
import com.ticketrush.inventory.security.AuthoritiesConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class InternalReservationResourceIT {

    private static final String API = "/api/internal/reservations";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketStockRepository ticketStockRepository;

    @Autowired
    private StockHoldRepository stockHoldRepository;

    private TicketStock stock;

    @BeforeEach
    void createStock() {
        stock = ticketStockRepository.saveAndFlush(new TicketStock().ticketTypeId(42L).eventId(1L).totalQuantity(2).availableQuantity(2));
    }

    @AfterEach
    void cleanup() {
        stockHoldRepository.deleteAll();
        ticketStockRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void endUsersCannotReserveDirectly() throws Exception {
        mockMvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(body(1, 1))).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void endUsersCannotEditStock() throws Exception {
        mockMvc.perform(delete("/api/ticket-stocks/{id}", stock.getId())).andExpect(status().isForbidden());
    }

    @Test
    void anonymousVisitorsSeeSeatsLeftButNothingElse() throws Exception {
        mockMvc
            .perform(get("/api/public/availability").param("ticketTypeIds", stock.getTicketTypeId() + ",999999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].ticketTypeId").value(stock.getTicketTypeId()))
            .andExpect(jsonPath("$[0].availableQuantity").value(2))
            .andExpect(jsonPath("$[0].totalQuantity").value(2));
        mockMvc.perform(get("/api/stock-holds")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.SERVICE)
    void reserveConfirmAndSoldOut() throws Exception {
        mockMvc
            .perform(post(API).contentType(MediaType.APPLICATION_JSON).content(body(1, 2)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderId").value(1))
            .andExpect(jsonPath("$.expiresAt").isNotEmpty())
            .andExpect(jsonPath("$.lines[0].quantity").value(2))
            .andExpect(jsonPath("$.lines[0].status").value("HELD"));

        mockMvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(body(2, 1))).andExpect(status().isConflict());

        mockMvc.perform(post(API + "/{orderId}/confirm", 1)).andExpect(status().isOk()).andExpect(jsonPath("$.lines[0].status").value("CONFIRMED"));
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.SERVICE)
    void rejectsInvalidRequests() throws Exception {
        mockMvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(body(1, 0))).andExpect(status().isBadRequest());
        mockMvc
            .perform(post(API).contentType(MediaType.APPLICATION_JSON).content("{\"orderId\":1,\"items\":[{\"ticketTypeId\":999,\"quantity\":1}]}"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post(API + "/{orderId}/confirm", 404)).andExpect(status().isConflict());
    }

    private String body(long orderId, int quantity) {
        return "{\"orderId\":" + orderId + ",\"items\":[{\"ticketTypeId\":" + stock.getTicketTypeId() + ",\"quantity\":" + quantity + "}]}";
    }
}
