package com.ticketrush.payment.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class PaymentTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Payment getPaymentSample1() {
        return new Payment()
            .id(1L)
            .orderId(1L)
            .orderCode("orderCode1")
            .currency("currency1")
            .providerRef("providerRef1")
            .providerTransactionId("providerTransactionId1")
            .paymentUrl("paymentUrl1")
            .failureReason("failureReason1");
    }

    public static Payment getPaymentSample2() {
        return new Payment()
            .id(2L)
            .orderId(2L)
            .orderCode("orderCode2")
            .currency("currency2")
            .providerRef("providerRef2")
            .providerTransactionId("providerTransactionId2")
            .paymentUrl("paymentUrl2")
            .failureReason("failureReason2");
    }

    public static Payment getPaymentRandomSampleGenerator() {
        return new Payment()
            .id(longCount.incrementAndGet())
            .orderId(longCount.incrementAndGet())
            .orderCode(UUID.randomUUID().toString())
            .currency(UUID.randomUUID().toString())
            .providerRef(UUID.randomUUID().toString())
            .providerTransactionId(UUID.randomUUID().toString())
            .paymentUrl(UUID.randomUUID().toString())
            .failureReason(UUID.randomUUID().toString());
    }
}
