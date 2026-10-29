package com.ticketrush.catalog.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class EventTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + 2L * Integer.MAX_VALUE);

    public static Event getEventSample1() {
        return new Event().id(1L).name("name1").slug("slug1").posterUrl("posterUrl1");
    }

    public static Event getEventSample2() {
        return new Event().id(2L).name("name2").slug("slug2").posterUrl("posterUrl2");
    }

    public static Event getEventRandomSampleGenerator() {
        return new Event()
            .id(longCount.incrementAndGet())
            .name(UUID.randomUUID().toString())
            .slug(UUID.randomUUID().toString())
            .posterUrl(UUID.randomUUID().toString());
    }
}
