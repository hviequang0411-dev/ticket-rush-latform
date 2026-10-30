package com.ticketrush.order.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ticketrush.order.IntegrationTest;
import com.ticketrush.order.domain.Order;
import com.ticketrush.order.domain.OrderItem;
import com.ticketrush.order.domain.enumeration.OrderStatus;
import com.ticketrush.order.repository.OrderItemRepository;
import com.ticketrush.order.repository.OrderRepository;
import com.ticketrush.order.repository.TicketRepository;
import com.ticketrush.order.security.AuthoritiesConstants;
import com.ticketrush.order.service.ticket.TicketService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class AdminOrderResourceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void cleanup() {
        ticketRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void searchByCodeOrBuyerAndStatus() throws Exception {
        Order alices = order("alice", OrderStatus.CONFIRMED, "1000000", 2, Instant.now());
        order("bob", OrderStatus.EXPIRED, "500000", 1, Instant.now());

        mockMvc
            .perform(get("/api/admin/orders").param("q", "ALI"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].userLogin").value("alice"));
        mockMvc
            .perform(get("/api/admin/orders").param("q", alices.getCode().substring(2, 8)))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].code").value(alices.getCode()));
        mockMvc
            .perform(get("/api/admin/orders").param("status", "EXPIRED"))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].userLogin").value("bob"));
        mockMvc.perform(get("/api/admin/orders")).andExpect(header().string("X-Total-Count", "2"));
    }

    @Test
    void orderDetailShowsItsTickets() throws Exception {
        Order order = order("alice", OrderStatus.CONFIRMED, "1000000", 2, Instant.now());
        transactionTemplate.executeWithoutResult(s -> ticketService.issue(orderRepository.findOneWithItemsById(order.getId()).orElseThrow()));

        mockMvc
            .perform(get("/api/admin/orders/" + order.getCode()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].quantity").value(2))
            .andExpect(jsonPath("$.tickets.length()").value(2));
        mockMvc.perform(get("/api/admin/orders/TRNOPE")).andExpect(status().isNotFound());
    }

    @Test
    void salesStatsCountConfirmedRevenueAndSuccessRate() throws Exception {
        Instant now = Instant.now();
        order("alice", OrderStatus.CONFIRMED, "1000000", 2, now);
        order("bob", OrderStatus.CONFIRMED, "500000", 1, now);
        order("carol", OrderStatus.EXPIRED, "700000", 1, now);
        order("dave", OrderStatus.AWAITING_PAYMENT, "300000", 1, now);
        // Two days ago: in the chart, not in today's figures
        order("erin", OrderStatus.CONFIRMED, "200000", 1, now.minus(Duration.ofDays(2)));

        mockMvc
            .perform(get("/api/admin/stats/sales").param("days", "7").param("zone", "UTC"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.today.revenue").value(1500000.0))
            .andExpect(jsonPath("$.today.ordersPlaced").value(4))
            .andExpect(jsonPath("$.today.ordersConfirmed").value(2))
            .andExpect(jsonPath("$.today.ordersFinished").value(3))
            .andExpect(jsonPath("$.activeHolds").value(1))
            .andExpect(jsonPath("$.daily.length()").value(7))
            .andExpect(jsonPath("$.daily[4].revenue").value(200000.0))
            .andExpect(jsonPath("$.daily[6].revenue").value(1500000.0))
            .andExpect(jsonPath("$.pace[0].eventId").value(1))
            .andExpect(jsonPath("$.pace[0].seats").value(3));
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void buyersSeeNeitherOtherOrdersNorFigures() throws Exception {
        mockMvc.perform(get("/api/admin/orders")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/stats/sales")).andExpect(status().isForbidden());
    }

    private Order order(String user, OrderStatus status, String total, int seats, Instant at) {
        Order order = orderRepository.save(
            new Order()
                .code("TR" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                .userLogin(user)
                .status(status)
                .totalAmount(new BigDecimal(total))
                .currency("VND")
                .idempotencyKey(UUID.randomUUID().toString())
                .createdAt(at)
                .updatedAt(at)
        );
        orderItemRepository.save(new OrderItem().eventId(1L).ticketTypeId(11L).quantity(seats).unitPrice(new BigDecimal(total)).order(order));
        return order;
    }
}
