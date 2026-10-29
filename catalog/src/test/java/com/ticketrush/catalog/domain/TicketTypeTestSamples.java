package com.ticketrush.catalog.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TicketTypeTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static TicketType getTicketTypeSample1() {
        return new TicketType().id(1L).name("name1").currency("currency1").totalQuantity(1);
    }

    public static TicketType getTicketTypeSample2() {
        return new TicketType().id(2L).name("name2").currency("currency2").totalQuantity(2);
    }

    public static TicketType getTicketTypeRandomSampleGenerator() {
        return new TicketType()
            .id(longCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .currency(UUID.randomUUID().toString())
            .totalQuantity(intCount.incrementAndGet());
    }
}
