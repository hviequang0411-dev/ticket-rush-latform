package com.ticketrush.catalog.service.outbox;

import com.ticketrush.catalog.domain.OutboxEvent;
import com.ticketrush.catalog.repository.OutboxEventRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Records an event for {@link OutboxRelay} to publish. Must run inside the transaction of the change it describes:
 * outside of one there is nothing to be atomic with, so it refuses.
 */
@Service
public class Outbox {

    private final OutboxEventRepository outboxEventRepository;

    private final ObjectMapper objectMapper;

    private final Clock clock;

    private final ObjectProvider<Tracer> tracer;

    private final ObjectProvider<Propagator> propagator;

    public Outbox(
        OutboxEventRepository outboxEventRepository,
        ObjectMapper objectMapper,
        Clock clock,
        ObjectProvider<Tracer> tracer,
        ObjectProvider<Propagator> propagator
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.tracer = tracer;
        this.propagator = propagator;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String topic, String aggregateType, Object aggregateId, String eventType, Object payload) {
        outboxEventRepository.save(
            new OutboxEvent(
                aggregateType,
                String.valueOf(aggregateId),
                eventType,
                topic,
                objectMapper.writeValueAsString(payload),
                clock.instant(),
                currentTraceParent()
            )
        );
    }

    private String currentTraceParent() {
        Tracer currentTracer = tracer.getIfAvailable();
        Propagator currentPropagator = propagator.getIfAvailable();
        Span span = currentTracer == null ? null : currentTracer.currentSpan();
        if (span == null || currentPropagator == null) {
            return null;
        }
        Map<String, String> carrier = new HashMap<>();
        currentPropagator.inject(span.context(), carrier, Map::put);
        return carrier.get(OutboxRelay.TRACEPARENT);
    }
}
