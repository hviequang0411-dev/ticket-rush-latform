package com.ticketrush.inventory.domain;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class StockHoldTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);
    private static final AtomicInteger intCount = new AtomicInteger(random.nextInt() + 2 * Short.MAX_VALUE);

    public static StockHold getStockHoldSample1() {
        return new StockHold().id(1L).orderId(1L).quantity(1);
    }

    public static StockHold getStockHoldSample2() {
        return new StockHold().id(2L).orderId(2L).quantity(2);
    }

    public static StockHold getStockHoldRandomSampleGenerator() {
        return new StockHold().id(longCount.incrementAndGet()).orderId(longCount.incrementAndGet()).quantity(intCount.incrementAndGet());
    }
}
