package com.ticketrush.payment.web.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ticketrush.payment.IntegrationTest;
import com.ticketrush.payment.domain.enumeration.PaymentProvider;
import com.ticketrush.payment.repository.OutboxEventRepository;
import com.ticketrush.payment.repository.PaymentRepository;
import com.ticketrush.payment.repository.PaymentWebhookEventRepository;
import com.ticketrush.payment.security.AuthoritiesConstants;
import com.ticketrush.payment.service.flow.PaymentFlowService;
import com.ticketrush.payment.service.flow.PaymentRequest;
import com.ticketrush.payment.service.flow.PaymentView;
import com.ticketrush.payment.service.provider.MockPaymentProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
@WithMockUser(authorities = AuthoritiesConstants.ADMIN)
class AdminPaymentResourceIT {

    private static final BigDecimal AMOUNT = new BigDecimal("1200000");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentFlowService paymentFlowService;

    @Autowired
    private MockPaymentProvider mockProvider;

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
    void retriedCallbacksShowOnceWithTheirDeliveryCount() throws Exception {
        PaymentView payment = paymentFlowService.create(
            new PaymentRequest(9001L, "TRADMIN9001", AMOUNT, "VND", Instant.now().plus(10, ChronoUnit.MINUTES))
        );
        Map<String, String> callback = mockProvider.callbackFor(payment.providerRef(), AMOUNT, MockPaymentProvider.SUCCESS);
        paymentFlowService.handleWebhook(PaymentProvider.MOCK, callback, null);
        paymentFlowService.handleWebhook(PaymentProvider.MOCK, callback, null);
        paymentFlowService.handleWebhook(PaymentProvider.MOCK, callback, null);

        mockMvc
            .perform(get("/api/admin/webhooks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].orderCode").value("TRADMIN9001"))
            .andExpect(jsonPath("$[0].deliveries").value(3))
            .andExpect(jsonPath("$[0].signatureValid").value(true))
            .andExpect(jsonPath("$[0].outcome").value("PROCESSED"))
            .andExpect(jsonPath("$[0].lastReceivedAt").isNotEmpty());

        mockMvc
            .perform(get("/api/admin/payments").param("q", "admin9001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("SUCCEEDED"));
        mockMvc.perform(get("/api/admin/payments").param("status", "FAILED")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(authorities = AuthoritiesConstants.USER)
    void buyersCannotSeePaymentsOrCallbacks() throws Exception {
        mockMvc.perform(get("/api/admin/payments")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/webhooks")).andExpect(status().isForbidden());
    }
}
