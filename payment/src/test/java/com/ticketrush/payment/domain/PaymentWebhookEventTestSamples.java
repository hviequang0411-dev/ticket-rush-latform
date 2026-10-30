package com.ticketrush.payment.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class PaymentWebhookEventTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static PaymentWebhookEvent getPaymentWebhookEventSample1() {
        return new PaymentWebhookEvent().id(1L).eventKey("eventKey1");
    }

    public static PaymentWebhookEvent getPaymentWebhookEventSample2() {
        return new PaymentWebhookEvent().id(2L).eventKey("eventKey2");
    }

    public static PaymentWebhookEvent getPaymentWebhookEventRandomSampleGenerator() {
        return new PaymentWebhookEvent().id(longCount.incrementAndGet()).eventKey(UUID.randomUUID().toString());
    }
}
