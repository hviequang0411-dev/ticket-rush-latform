package com.ticketrush.payment.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketrush.payment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PaymentWebhookEventDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(PaymentWebhookEventDTO.class);
        PaymentWebhookEventDTO paymentWebhookEventDTO1 = new PaymentWebhookEventDTO();
        paymentWebhookEventDTO1.setId(1L);
        PaymentWebhookEventDTO paymentWebhookEventDTO2 = new PaymentWebhookEventDTO();
        assertThat(paymentWebhookEventDTO1).isNotEqualTo(paymentWebhookEventDTO2);
        paymentWebhookEventDTO2.setId(paymentWebhookEventDTO1.getId());
        assertThat(paymentWebhookEventDTO1).isEqualTo(paymentWebhookEventDTO2);
        paymentWebhookEventDTO2.setId(2L);
        assertThat(paymentWebhookEventDTO1).isNotEqualTo(paymentWebhookEventDTO2);
        paymentWebhookEventDTO1.setId(null);
        assertThat(paymentWebhookEventDTO1).isNotEqualTo(paymentWebhookEventDTO2);
    }
}
