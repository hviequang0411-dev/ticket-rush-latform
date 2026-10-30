package com.ticketrush.inventory.service.outbox;

import com.ticketrush.inventory.domain.OutboxEvent;
import com.ticketrush.inventory.repository.OutboxEventRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.MimeTypeUtils;

/**
 * Moves outbox events to Kafka: at-least-once, in creation order.
 * <p>
 * A batch is locked, sent and marked published in one transaction. Producers are synchronous
 * ({@code spring.cloud.stream.kafka.default.producer.sync}), so an event is only marked once Kafka acknowledged
 * it. If a send fails the batch stops there, so later events never overtake an earlier one; the failed event is
 * retried on the next run. A crash after the send but before the commit resends the batch, which is why
 * consumers deduplicate on the {@value #EVENT_ID} header.
 */
@Component
public class OutboxRelay {

    public static final String EVENT_ID = "eventId";

    public static final String EVENT_TYPE = "eventType";

    private static final Logger LOG = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository outboxEventRepository;

    private final StreamBridge streamBridge;

    private final TransactionTemplate transactionTemplate;

    private final Clock clock;

    private final int batchSize;

    private final Duration retention;

    private final boolean scheduled;

    public OutboxRelay(
        OutboxEventRepository outboxEventRepository,
        StreamBridge streamBridge,
        TransactionTemplate transactionTemplate,
        Clock clock,
        @Value("${ticketrush.outbox.batch-size:100}") int batchSize,
        @Value("${ticketrush.outbox.retention:P7D}") Duration retention,
        @Value("${ticketrush.outbox.relay-enabled:true}") boolean scheduled
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.streamBridge = streamBridge;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.batchSize = batchSize;
        this.retention = retention;
        this.scheduled = scheduled;
    }

    @Scheduled(fixedDelayString = "${ticketrush.outbox.poll-interval:PT0.5S}")
    public void relay() {
        if (!scheduled) {
            return;
        }
        int sent;
        do {
            sent = relayBatch();
        } while (sent == batchSize);
    }

    /**
     * @return how many events were published; less than the batch size when the outbox is drained or a send failed.
     */
    public int relayBatch() {
        Integer sent = transactionTemplate.execute(status -> {
            List<OutboxEvent> batch = outboxEventRepository.lockUnpublished(batchSize);
            int published = 0;
            for (OutboxEvent event : batch) {
                try {
                    if (!streamBridge.send(event.getTopic(), toMessage(event))) {
                        throw new IllegalStateException("Binder refused the message");
                    }
                } catch (RuntimeException e) {
                    LOG.warn("Could not publish outbox event {} ({}) to {}: {}", event.getId(), event.getEventType(), event.getTopic(), e.getMessage());
                    event.markFailed(e.getMessage());
                    break;
                }
                event.markPublished(clock.instant());
                published++;
            }
            return published;
        });
        return sent == null ? 0 : sent;
    }

    @Scheduled(cron = "${ticketrush.outbox.cleanup-cron:0 30 3 * * *}")
    public void deletePublished() {
        Integer deleted = transactionTemplate.execute(status -> outboxEventRepository.deletePublishedBefore(clock.instant().minus(retention)));
        LOG.info("Deleted {} published outbox events older than {}", deleted, retention);
    }

    private static Message<byte[]> toMessage(OutboxEvent event) {
        return MessageBuilder.withPayload(event.getPayload().getBytes(StandardCharsets.UTF_8))
            .setHeader(KafkaHeaders.KEY, event.getAggregateId().getBytes(StandardCharsets.UTF_8))
            .setHeader(EVENT_ID, event.getId().toString())
            .setHeader(EVENT_TYPE, event.getEventType())
            .setHeader("contentType", MimeTypeUtils.APPLICATION_JSON_VALUE)
            .build();
    }
}
