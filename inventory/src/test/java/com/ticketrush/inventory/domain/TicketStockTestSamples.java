package com.ticketrush.inventory.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TicketStockTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static TicketStock getTicketStockSample1() {
        return new TicketStock().id(1L).ticketTypeId(1L).eventId(1L).totalQuantity(1).availableQuantity(1);
    }

    public static TicketStock getTicketStockSample2() {
        return new TicketStock().id(2L).ticketTypeId(2L).eventId(2L).totalQuantity(2).availableQuantity(2);
    }

    public static TicketStock getTicketStockRandomSampleGenerator() {
        return new TicketStock()
            .id(longCount.incrementAndGet())
            .ticketTypeId(longCount.incrementAndGet())
            .eventId(longCount.incrementAndGet())
            .totalQuantity(intCount.incrementAndGet())
            .availableQuantity(intCount.incrementAndGet());
    }
}
