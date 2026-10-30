package com.ticketrush.inventory.broker;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketrush.inventory.IntegrationTest;
import com.ticketrush.inventory.domain.TicketStock;
import com.ticketrush.inventory.repository.StockHoldRepository;
import com.ticketrush.inventory.repository.TicketStockRepository;
import com.ticketrush.inventory.service.reservation.ReservationRequest;
import com.ticketrush.inventory.service.reservation.ReservationService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;

@IntegrationTest
@ImportAutoConfiguration(TestChannelBinderConfiguration.class)
// The Kafka binder is on the classpath too; route messages to the in-memory test binder
@TestPropertySource(properties = "spring.cloud.stream.default-binder=integration")
class TicketTypeEventsConsumerIT {

    private static final String TOPIC = "catalog.ticket-type.v1";

    private static final long TICKET_TYPE_ID = 7_000L;

    @Autowired
    private InputDestination input;

    @Autowired
    private TicketStockRepository ticketStockRepository;

    @Autowired
    private StockHoldRepository stockHoldRepository;

    @Autowired
    private ReservationService reservationService;

    @AfterEach
    void cleanup() {
        stockHoldRepository.deleteAll();
        ticketStockRepository.deleteAll();
    }

    @Test
    void createsAndResizesStockIdempotently() {
        send(TicketTypeEvent.Type.CREATED, 100);
        assertThat(stock()).satisfies(stock -> {
            assertThat(stock.getTotalQuantity()).isEqualTo(100);
            assertThat(stock.getAvailableQuantity()).isEqualTo(100);
        });

        reservationService.reserve(new ReservationRequest(1L, List.of(new ReservationRequest.Item(TICKET_TYPE_ID, 30))));

        send(TicketTypeEvent.Type.UPDATED, 120);
        send(TicketTypeEvent.Type.UPDATED, 120);
        assertThat(stock()).satisfies(stock -> {
            assertThat(stock.getTotalQuantity()).isEqualTo(120);
            assertThat(stock.getAvailableQuantity()).isEqualTo(90);
        });

        // Shrinking below what is already held stops sales instead of un-selling seats
        send(TicketTypeEvent.Type.UPDATED, 10);
        assertThat(stock()).satisfies(stock -> {
            assertThat(stock.getTotalQuantity()).isEqualTo(30);
            assertThat(stock.getAvailableQuantity()).isZero();
        });
    }

    @Test
    void staleRedeliveryIsIgnored() {
        send(TicketTypeEvent.Type.CREATED, 100);
        UUID resizeTo120 = UUID.randomUUID();
        send(TicketTypeEvent.Type.UPDATED, 120, resizeTo120);
        send(TicketTypeEvent.Type.UPDATED, 80);

        // The outbox delivers at least once: the 120 event shows up again after the newer 80 one
        send(TicketTypeEvent.Type.UPDATED, 120, resizeTo120);

        assertThat(stock().getTotalQuantity()).isEqualTo(80);
    }

    private void send(TicketTypeEvent.Type type, int total) {
        send(type, total, UUID.randomUUID());
    }

    private void send(TicketTypeEvent.Type type, int total, UUID eventId) {
        input.send(
            MessageBuilder.withPayload(new TicketTypeEvent(type, TICKET_TYPE_ID, 1L, total, Instant.now()))
                .setHeader(TicketTypeEventsConsumer.EVENT_ID, eventId.toString())
                .build(),
            TOPIC
        );
    }

    private TicketStock stock() {
        return ticketStockRepository.findByTicketTypeId(TICKET_TYPE_ID).orElseThrow();
    }
}
