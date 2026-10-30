package com.ticketrush.inventory.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ticketrush.inventory.IntegrationTest;
import com.ticketrush.inventory.domain.TicketStock;
import com.ticketrush.inventory.repository.StockHoldRepository;
import com.ticketrush.inventory.repository.TicketStockRepository;
import com.ticketrush.inventory.security.AuthoritiesConstants;
import com.ticketrush.inventory.service.reservation.ReservationRequest;
import com.ticketrush.inventory.service.reservation.ReservationService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class AdminStockResourceIT {

    private static final long TYPE = 77L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private TicketStockRepository ticketStockRepository;

    @Autowired
    private StockHoldRepository stockHoldRepository;

    @BeforeEach
    void createStock() {
        ticketStockRepository.saveAndFlush(new TicketStock().ticketTypeId(TYPE).eventId(5L).totalQuantity(10).availableQuantity(10));
    }

    @AfterEach
    void cleanup() {
        stockHoldRepository.deleteAll();
        ticketStockRepository.deleteAll();
    }

    @Test
    void everySeatIsSoldHeldOrLeft() throws Exception {
        reservationService.reserve(new ReservationRequest(1L, List.of(new ReservationRequest.Item(TYPE, 3))));
        reservationService.confirm(1L);
        reservationService.reserve(new ReservationRequest(2L, List.of(new ReservationRequest.Item(TYPE, 2))));
        reservationService.reserve(new ReservationRequest(3L, List.of(new ReservationRequest.Item(TYPE, 1))));
        reservationService.release(3L);

        mockMvc
            .perform(get("/api/admin/stock").param("ticketTypeIds", TYPE + ",999"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].eventId").value(5))
            .andExpect(jsonPath("$[0].totalQuantity").value(10))
            .andExpect(jsonPath("$[0].soldQuantity").value(3))
            .andExpect(jsonPath("$[0].heldQuantity").value(2))
            .andExpect(jsonPath("$[0].availableQuantity").value(5));

        mockMvc
            .perform(get("/api/admin/holds/expiring"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].orderId").value(2))
            .andExpect(jsonPath("$[0].quantity").value(2))
            .andExpect(jsonPath("$[0].expiresAt").isNotEmpty());
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void buyersCannotSeeStockDetails() throws Exception {
        mockMvc.perform(get("/api/admin/stock").param("ticketTypeIds", String.valueOf(TYPE))).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/holds/expiring")).andExpect(status().isForbidden());
    }
}
