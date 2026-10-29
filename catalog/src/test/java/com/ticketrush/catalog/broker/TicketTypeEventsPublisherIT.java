package com.ticketrush.catalog.broker;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketrush.catalog.IntegrationTest;
import com.ticketrush.catalog.domain.Event;
import com.ticketrush.catalog.domain.Venue;
import com.ticketrush.catalog.domain.enumeration.EventStatus;
import com.ticketrush.catalog.repository.EventRepository;
import com.ticketrush.catalog.repository.TicketTypeRepository;
import com.ticketrush.catalog.repository.VenueRepository;
import com.ticketrush.catalog.service.TicketTypeService;
import com.ticketrush.catalog.service.dto.EventDTO;
import com.ticketrush.catalog.service.dto.TicketTypeDTO;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@IntegrationTest
@ImportAutoConfiguration(TestChannelBinderConfiguration.class)
// The Kafka binder is on the classpath too; route messages to the in-memory test binder
@TestPropertySource(properties = "spring.cloud.stream.default-binder=integration")
class TicketTypeEventsPublisherIT {

    private static final String TOPIC = "catalog.ticket-type.v1";

    @Autowired
    private TicketTypeService ticketTypeService;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

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
                .slug("publisher-" + UUID.randomUUID())
                .startTime(Instant.parse("2026-12-31T12:00:00Z"))
                .status(EventStatus.PUBLISHED)
                .venue(venue)
        );
        output.clear();
    }

    @AfterEach
    void cleanup() {
        ticketTypeRepository.deleteAll();
        eventRepository.deleteAll();
        venueRepository.deleteAll();
    }

    @Test
    void publishesCreatedAndUpdatedAfterCommit() throws Exception {
        TicketTypeDTO created = ticketTypeService.save(ticketType(100));

        JsonNode createdEvent = receive();
        assertThat(createdEvent.get("type").asString()).isEqualTo("CREATED");
        assertThat(createdEvent.get("ticketTypeId").asLong()).isEqualTo(created.getId());
        assertThat(createdEvent.get("eventId").asLong()).isEqualTo(event.getId());
        assertThat(createdEvent.get("totalQuantity").asInt()).isEqualTo(100);

        TicketTypeDTO patch = new TicketTypeDTO();
        patch.setId(created.getId());
        patch.setTotalQuantity(150);
        ticketTypeService.partialUpdate(patch);

        JsonNode updatedEvent = receive();
        assertThat(updatedEvent.get("type").asString()).isEqualTo("UPDATED");
        assertThat(updatedEvent.get("totalQuantity").asInt()).isEqualTo(150);
    }

    @Test
    void publishesNothingWhenTheTransactionRollsBack() {
        transactionTemplate.executeWithoutResult(status -> {
            ticketTypeService.save(ticketType(10));
            status.setRollbackOnly();
        });

        assertThat(output.receive(1000, TOPIC)).isNull();
    }

    private JsonNode receive() throws Exception {
        Message<byte[]> message = output.receive(5000, TOPIC);
        assertThat(message).as("message on " + TOPIC).isNotNull();
        return om.readTree(message.getPayload());
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
