package com.ticketrush.inventory.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remembers which Kafka events a consumer already handled. Kafka and the outbox deliver at least once, so the same
 * event can arrive twice; recording it in the transaction that applies it makes the second delivery a no-op.
 */
@Repository
public class ProcessedEvents {

    private final JdbcTemplate jdbcTemplate;

    public ProcessedEvents(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * @return true the first time {@code consumer} sees {@code eventId}, false for a redelivery.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean firstDelivery(String consumer, UUID eventId, Instant now) {
        return (
            jdbcTemplate.update(
                "insert into processed_event (consumer, event_id, processed_at) values (?, ?, ?) on conflict do nothing",
                consumer,
                eventId,
                Timestamp.from(now)
            ) ==
            1
        );
    }
}
