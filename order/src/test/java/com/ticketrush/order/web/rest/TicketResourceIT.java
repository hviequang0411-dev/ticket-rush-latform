package com.ticketrush.order.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ticketrush.order.IntegrationTest;
import com.ticketrush.order.domain.Order;
import com.ticketrush.order.domain.OrderItem;
import com.ticketrush.order.domain.enumeration.OrderStatus;
import com.ticketrush.order.repository.OrderItemRepository;
import com.ticketrush.order.repository.OrderRepository;
import com.ticketrush.order.repository.TicketRepository;
import com.ticketrush.order.security.AuthoritiesConstants;
import com.ticketrush.order.service.ticket.CheckInResult;
import com.ticketrush.order.service.ticket.TicketService;
import com.ticketrush.order.service.ticket.TicketView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@IntegrationTest
@AutoConfigureMockMvc
class TicketResourceIT {

    private static final int GATES = 16;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void cleanup() {
        ticketRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void confirmedOrderGetsOneTicketPerSeatAndOnlyOnce() {
        Order order = confirmedOrder("alice", 2, 1);

        issue(order);
        issue(order);

        List<TicketView> tickets = ticketService.findByOrder(order.getId());
        assertThat(tickets).hasSize(3);
        assertThat(tickets).extracting(TicketView::code).doesNotHaveDuplicates();
        assertThat(tickets).allSatisfy(t -> assertThat(t.qrPayload()).startsWith("TR1." + t.code() + "."));
    }

    @Test
    void manyGatesScanningTheSameTicketAdmitItOnce() throws Exception {
        Order order = confirmedOrder("alice", 1);
        issue(order);
        String qr = ticketService.findByOrder(order.getId()).getFirst().qrPayload();

        CountDownLatch start = new CountDownLatch(1);
        List<Callable<CheckInResult>> scans = new ArrayList<>();
        for (int gate = 0; gate < GATES; gate++) {
            String staff = "gate-" + gate;
            scans.add(() -> {
                start.await();
                return ticketService.checkIn(qr, staff);
            });
        }
        List<CheckInResult.Outcome> outcomes = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(GATES)) {
            List<Future<CheckInResult>> futures = scans.stream().map(pool::submit).toList();
            start.countDown();
            for (Future<CheckInResult> future : futures) {
                outcomes.add(future.get().outcome());
            }
        }

        assertThat(outcomes).filteredOn(o -> o == CheckInResult.Outcome.ADMITTED).hasSize(1);
        assertThat(outcomes).filteredOn(o -> o == CheckInResult.Outcome.ALREADY_USED).hasSize(GATES - 1);
    }

    @Test
    void editedOrMadeUpCodesAreRejected() {
        Order order = confirmedOrder("alice", 1);
        issue(order);
        TicketView ticket = ticketService.findByOrder(order.getId()).getFirst();
        String forged = "TR1." + ticket.code() + ".AAAAAAAAAAAAAAAAAAAAAA";

        assertThat(ticketService.checkIn(forged, "gate").outcome()).isEqualTo(CheckInResult.Outcome.INVALID);
        assertThat(ticketService.checkIn("hello", "gate").outcome()).isEqualTo(CheckInResult.Outcome.INVALID);
        assertThat(ticketRepository.findOneByCode(ticket.code()).orElseThrow().getCheckedInAt()).isNull();
    }

    @Test
    @WithMockUser("alice")
    void holdersSeeOnlyTheirOwnTickets() throws Exception {
        issue(confirmedOrder("alice", 2));
        issue(confirmedOrder("bob", 1));

        mockMvc
            .perform(get("/api/my-tickets"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].userLogin").value("alice"))
            .andExpect(jsonPath("$[0].status").value("VALID"));
    }

    @Test
    @WithMockUser("alice")
    void buyersCannotScanTickets() throws Exception {
        mockMvc
            .perform(post("/api/admin/check-in").contentType(MediaType.APPLICATION_JSON).content("{\"qrPayload\":\"TR1.x.y\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(value = "staff", authorities = AuthoritiesConstants.ADMIN)
    void staffScanAtTheGate() throws Exception {
        Order order = confirmedOrder("alice", 1);
        issue(order);
        String qr = ticketService.findByOrder(order.getId()).getFirst().qrPayload();
        String body = "{\"qrPayload\":\"" + qr + "\"}";

        mockMvc
            .perform(post("/api/admin/check-in").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome").value("ADMITTED"))
            .andExpect(jsonPath("$.ticket.orderCode").value(order.getCode()));
        mockMvc
            .perform(post("/api/admin/check-in").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome").value("ALREADY_USED"))
            .andExpect(jsonPath("$.ticket.checkedInAt").isNotEmpty());
    }

    private void issue(Order order) {
        transactionTemplate.executeWithoutResult(status -> ticketService.issue(orderRepository.findOneWithItemsById(order.getId()).orElseThrow()));
    }

    private Order confirmedOrder(String user, int... quantities) {
        Order order = orderRepository.save(
            new Order()
                .code("TR" + UUID.randomUUID().toString().substring(0, 10).toUpperCase())
                .userLogin(user)
                .status(OrderStatus.CONFIRMED)
                .totalAmount(new BigDecimal("1000000"))
                .currency("VND")
                .idempotencyKey(UUID.randomUUID().toString())
                .createdAt(Instant.now())
        );
        long ticketType = 11;
        for (int quantity : quantities) {
            orderItemRepository.save(
                new OrderItem().eventId(1L).ticketTypeId(ticketType++).quantity(quantity).unitPrice(new BigDecimal("500000")).order(order)
            );
        }
        return order;
    }
}
