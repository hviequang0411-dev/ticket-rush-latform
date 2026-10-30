package com.ticketrush.payment.domain;

import static com.ticketrush.payment.domain.PaymentTestSamples.*;
import static com.ticketrush.payment.domain.PaymentWebhookEventTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.ticketrush.payment.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class PaymentWebhookEventTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(PaymentWebhookEvent.class);
        PaymentWebhookEvent paymentWebhookEvent1 = getPaymentWebhookEventSample1();
        PaymentWebhookEvent paymentWebhookEvent2 = new PaymentWebhookEvent();
        assertThat(paymentWebhookEvent1).isNotEqualTo(paymentWebhookEvent2);

        paymentWebhookEvent2.setId(paymentWebhookEvent1.getId());
        assertThat(paymentWebhookEvent1).isEqualTo(paymentWebhookEvent2);

        paymentWebhookEvent2 = getPaymentWebhookEventSample2();
        assertThat(paymentWebhookEvent1).isNotEqualTo(paymentWebhookEvent2);
    }

    @Test
    void paymentTest() {
        PaymentWebhookEvent paymentWebhookEvent = getPaymentWebhookEventRandomSampleGenerator();
        Payment paymentBack = getPaymentRandomSampleGenerator();

        paymentWebhookEvent.setPayment(paymentBack);
        assertThat(paymentWebhookEvent.getPayment()).isEqualTo(paymentBack);

        paymentWebhookEvent.payment(null);
        assertThat(paymentWebhookEvent.getPayment()).isNull();
    }
}
