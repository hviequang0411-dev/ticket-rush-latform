package com.ticketrush.catalog.service.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketrush.catalog.IntegrationTest;
import com.ticketrush.catalog.domain.Event;
import com.ticketrush.catalog.domain.Venue;
import com.ticketrush.catalog.domain.enumeration.EventStatus;
import com.ticketrush.catalog.repository.EventRepository;
import com.ticketrush.catalog.repository.OutboxEventRepository;
import com.ticketrush.catalog.repository.TicketTypeRepository;
import com.ticketrush.catalog.repository.VenueRepository;
import com.ticketrush.catalog.service.TicketTypeService;
import com.ticketrush.catalog.service.dto.EventDTO;
import com.ticketrush.catalog.service.dto.TicketTypeDTO;
import com.ticketrush.catalog.service.event.TicketTypeChanged;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest
@ImportAutoConfiguration(TestChannelBinderConfiguration.class)
// The Kafka binder is on the classpath too; route messages to the in-memory test binder
@TestPropertySource(properties = "spring.cloud.stream.default-binder=integration")
class TicketTypeOutboxIT {

    private static final String TOPIC = TicketTypeChanged.TOPIC;

    @Autowired
    private TicketTypeService ticketTypeService;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private Outbox outbox;

    @Autowired
    private OutboxRelay outboxRelay;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private OutputDestination output;

    @Autowired
    private ObjectMapper om;

    private Event event;

    @BeforeEach
    void createEvent() {
        Venue venue = venueRepository.save(new Venue().name("Hanoi Arena").address("Me Tri").city("Ha Noi").capacity(30000));
        event = eventRepository.save(
            new Event()
                .name("Rush Live")
                .slug("outbox-" + UUID.randomUUID())
                .startTime(Instant.parse("2026-12-31T12:00:00Z"))
                .status(EventStatus.PUBLISHED)
                .venue(venue)
        );
        outboxEventRepository.deleteAll();
        output.clear();
    }

    @AfterEach
    void cleanup() {
        outboxEventRepository.deleteAll();
        ticketTypeRepository.deleteAll();
        eventRepository.deleteAll();
        venueRepository.deleteAll();
    }

    @Test
    void changesAreRecordedInTheOutboxAndRelayedInOrder() throws Exception {
        TicketTypeDTO created = ticketTypeService.save(ticketType(100));
        TicketTypeDTO patch = new TicketTypeDTO();
        patch.setId(created.getId());
        patch.setTotalQuantity(150);
        ticketTypeService.partialUpdate(patch);

        assertThat(output.receive(200, TOPIC)).as("nothing is sent before the relay runs").isNull();
        assertThat(outboxEventRepository.countByPublishedAtIsNull()).isEqualTo(2);

        assertThat(outboxRelay.relayBatch()).isEqualTo(2);

        Message<byte[]> first = output.receive(5000, TOPIC);
        JsonNode createdEvent = om.readTree(first.getPayload());
        assertThat(createdEvent.get("type").asString()).isEqualTo("CREATED");
        assertThat(createdEvent.get("ticketTypeId").asLong()).isEqualTo(created.getId());
        assertThat(createdEvent.get("eventId").asLong()).isEqualTo(event.getId());
        assertThat(createdEvent.get("totalQuantity").asInt()).isEqualTo(100);
        assertThat(first.getHeaders().get(OutboxRelay.EVENT_ID)).isNotNull();
        assertThat(new String((byte[]) first.getHeaders().get(KafkaHeaders.KEY), StandardCharsets.UTF_8)).isEqualTo(created.getId().toString());

        Message<byte[]> second = output.receive(5000, TOPIC);
        assertThat(om.readTree(second.getPayload()).get("type").asString()).isEqualTo("UPDATED");
        assertThat(second.getHeaders().get(OutboxRelay.EVENT_ID)).isNotEqualTo(first.getHeaders().get(OutboxRelay.EVENT_ID));

        assertThat(outboxEventRepository.countByPublishedAtIsNull()).isZero();
        assertThat(outboxRelay.relayBatch()).as("published events are not sent again").isZero();
    }

    @Test
    void rolledBackChangesLeaveNoEvent() {
        transactionTemplate.executeWithoutResult(status -> {
            ticketTypeService.save(ticketType(10));
            status.setRollbackOnly();
        });

        assertThat(outboxEventRepository.count()).isZero();
        assertThat(outboxRelay.relayBatch()).isZero();
        assertThat(output.receive(500, TOPIC)).isNull();
    }

    @Test
    void appendingOutsideATransactionIsRefused() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> outbox.append(TOPIC, "TicketType", 1L, "CREATED", "{}")).isInstanceOf(
            IllegalTransactionStateException.class
        );
    }

    private TicketTypeDTO ticketType(int quantity) {
        EventDTO eventRef = new EventDTO();
        eventRef.setId(event.getId());
        TicketTypeDTO dto = new TicketTypeDTO();
        dto.setName("GA");
        dto.setPrice(new BigDecimal("500000"));
        dto.setCurrency("VND");
        dto.setTotalQuantity(quantity);
        dto.setEvent(eventRef);
        return dto;
    }
}
