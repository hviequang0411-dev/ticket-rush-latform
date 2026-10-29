package com.ticketrush.order.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class OrderTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Order getOrderSample1() {
        return new Order()
            .id(1L)
            .code("code1")
            .userLogin("userLogin1")
            .currency("currency1")
            .idempotencyKey("idempotencyKey1")
            .failureReason("failureReason1");
    }

    public static Order getOrderSample2() {
        return new Order()
            .id(2L)
            .code("code2")
            .userLogin("userLogin2")
            .currency("currency2")
            .idempotencyKey("idempotencyKey2")
            .failureReason("failureReason2");
    }

    public static Order getOrderRandomSampleGenerator() {
        return new Order()
            .id(longCount.incrementAndGet())
            .code(UUID.randomUUID().toString())
            .userLogin(UUID.randomUUID().toString())
            .currency(UUID.randomUUID().toString())
            .idempotencyKey(UUID.randomUUID().toString())
            .failureReason(UUID.randomUUID().toString());
    }
}
