package com.ticketrush.inventory.service.outbox;

import com.ticketrush.inventory.domain.OutboxEvent;
import com.ticketrush.inventory.repository.OutboxEventRepository;
import java.time.Clock;
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

    public Outbox(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper, Clock clock) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(String topic, String aggregateType, Object aggregateId, String eventType, Object payload) {
        outboxEventRepository.save(
            new OutboxEvent(aggregateType, String.valueOf(aggregateId), eventType, topic, objectMapper.writeValueAsString(payload), clock.instant())
        );
    }
}
