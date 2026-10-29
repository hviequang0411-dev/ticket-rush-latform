package com.ticketrush.order.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ticketrush.order.IntegrationTest;
import com.ticketrush.order.client.CatalogClient;
import com.ticketrush.order.client.InventoryClient;
import com.ticketrush.order.repository.OrderItemRepository;
import com.ticketrush.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser("alice")
class CheckoutResourceIT {

    private static final String BODY = "{\"items\":[{\"ticketTypeId\":11,\"quantity\":2}]}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean
    private CatalogClient catalogClient;

    @MockitoBean
    private InventoryClient inventoryClient;

    @BeforeEach
    void stubDependencies() {
        when(catalogClient.getTicketType(11L)).thenReturn(
            new CatalogClient.TicketType(11L, "VIP", new BigDecimal("2500000"), "VND", null, null, new CatalogClient.EventRef(1L))
        );
        when(inventoryClient.reserve(any())).thenAnswer(invocation ->
            new InventoryClient.Reservation(((InventoryClient.ReserveRequest) invocation.getArgument(0)).orderId(), Instant.now(), List.of())
        );
    }

    @AfterEach
    void cleanup() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void checkoutCreatesThenReplays() throws Exception {
        String code = com.jayway.jsonpath.JsonPath.read(
            mockMvc
                .perform(post("/api/checkout").header("Idempotency-Key", "key-0001-abcd").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("RESERVED"))
                .andExpect(jsonPath("$.totalAmount").value(5000000))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "$.code"
        );

        mockMvc
            .perform(post("/api/checkout").header("Idempotency-Key", "key-0001-abcd").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(code));

        mockMvc.perform(get("/api/my-orders")).andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value(code));
        mockMvc.perform(post("/api/my-orders/{code}/cancel", code)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void idempotencyKeyIsRequiredAndValidated() throws Exception {
        mockMvc.perform(post("/api/checkout").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isBadRequest());
        mockMvc
            .perform(post("/api/checkout").header("Idempotency-Key", "short").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isBadRequest());
    }

    @Test
    void invalidItemsAreRejected() throws Exception {
        mockMvc
            .perform(
                post("/api/checkout")
                    .header("Idempotency-Key", "key-0002-abcd")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"items\":[{\"ticketTypeId\":11,\"quantity\":0}]}")
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void otherUsersOrdersAreInvisible() throws Exception {
        mockMvc.perform(get("/api/my-orders/{code}", "TRNOTMINE01")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/orders")).andExpect(status().isForbidden());
    }

    @Test
    void loadBalancedBuilderDoesNotReplaceTheDefaultOne() {
        // Spring Boot's plain builder must still exist for non-Consul hosts such as Keycloak
        Map<String, RestClient.Builder> builders = applicationContext.getBeansOfType(RestClient.Builder.class);
        org.assertj.core.api.Assertions.assertThat(builders).containsKey("loadBalancedRestClientBuilder").hasSizeGreaterThanOrEqualTo(2);
    }
}
