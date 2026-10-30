package com.ticketrush.payment.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ticketrush.payment.IntegrationTest;
import com.ticketrush.payment.repository.OutboxEventRepository;
import com.ticketrush.payment.repository.PaymentRepository;
import com.ticketrush.payment.repository.PaymentWebhookEventRepository;
import com.ticketrush.payment.security.AuthoritiesConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class PaymentApiSecurityIT {

    private static final String BODY =
        "{\"orderId\":42,\"orderCode\":\"TR42\",\"amount\":100000,\"currency\":\"VND\",\"expiresAt\":\"2099-01-01T00:00:00Z\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentWebhookEventRepository webhookEventRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @AfterEach
    void cleanup() {
        webhookEventRepository.deleteAll();
        paymentRepository.deleteAll();
        outboxEventRepository.deleteAll();
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void buyersCannotCreatePaymentsOrReadOthers() throws Exception {
        mockMvc.perform(post("/api/internal/payments").contentType(MediaType.APPLICATION_JSON).content(BODY)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/payments")).andExpect(status().isForbidden());
    }

    @Test
    void mockProviderFlowWorksWithoutAToken() throws Exception {
        String ref = createAsService();

        mockMvc.perform(get("/api/public/mock-provider/{ref}", ref)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString(ref)));
        mockMvc
            .perform(post("/api/public/mock-provider/{ref}/complete", ref).param("result", "success"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }

    @Test
    void forgedWebhookIsRejected() throws Exception {
        String ref = createAsService();

        mockMvc
            .perform(
                post("/api/public/webhooks/mock")
                    .param("ref", ref)
                    .param("result", "success")
                    .param("amount", "100000")
                    .param("transactionId", "FORGED-1")
                    .param("signature", "deadbeef")
            )
            .andExpect(status().isBadRequest());
    }

    private String createAsService() throws Exception {
        String json = mockMvc
            .perform(
                post("/api/internal/payments")
                    .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("order").authorities(
                        new org.springframework.security.core.authority.SimpleGrantedAuthority(AuthoritiesConstants.SERVICE)
                    ))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(BODY)
            )
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$.providerRef");
    }
}
