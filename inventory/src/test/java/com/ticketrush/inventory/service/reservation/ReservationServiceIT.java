package com.ticketrush.inventory.service.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketrush.inventory.IntegrationTest;
import com.ticketrush.inventory.domain.StockHold;
import com.ticketrush.inventory.domain.TicketStock;
import com.ticketrush.inventory.domain.enumeration.HoldStatus;
import com.ticketrush.inventory.repository.StockHoldRepository;
import com.ticketrush.inventory.repository.TicketStockRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

/**
 * Runs against a real PostgreSQL (Testcontainers) with real concurrent transactions: no {@code @Transactional} on
 * the test, and a connection pool big enough that requests genuinely race for the same row.
 */
@IntegrationTest
@Import(ReservationServiceIT.ClockConfig.class)
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=20")
class ReservationServiceIT {

    private static final AtomicLong IDS = new AtomicLong(1_000_000);

    @TestConfiguration
    static class ClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private TicketStockRepository ticketStockRepository;

    @Autowired
    private StockHoldRepository stockHoldRepository;

    @Autowired
    private Clock clock;

    @AfterEach
    void cleanup() {
        stockHoldRepository.deleteAll();
        ticketStockRepository.deleteAll();
    }

    @Test
    void concurrentReservationsNeverOversell() throws Exception {
        int seats = 50;
        int buyers = 200;
        TicketStock stock = createStock(seats);

        List<Outcome> outcomes = race(buyers, i -> () -> reserve(nextOrderId(), item(stock, 1)));

        assertThat(outcomes).filteredOn(Outcome.SUCCESS::equals).hasSize(seats);
        assertThat(outcomes).filteredOn(Outcome.SOLD_OUT::equals).hasSize(buyers - seats);
        assertThat(available(stock)).isZero();
        assertThat(stockHoldRepository.findAll()).hasSize(seats).allMatch(hold -> hold.getStatus() == HoldStatus.HELD);
    }

    @Test
    void concurrentMultiItemOrdersDoNotDeadlockOrOversell() throws Exception {
        TicketStock vip = createStock(30);
        TicketStock standard = createStock(30);

        // Half the buyers list the items in the opposite order; without a fixed lock order this deadlocks
        List<Outcome> outcomes = race(100, i ->
            () -> i % 2 == 0 ? reserve(nextOrderId(), item(vip, 1), item(standard, 1)) : reserve(nextOrderId(), item(standard, 1), item(vip, 1))
        );

        assertThat(outcomes).containsOnly(Outcome.SUCCESS, Outcome.SOLD_OUT);
        assertThat(outcomes).filteredOn(Outcome.SUCCESS::equals).hasSize(30);
        assertThat(available(vip)).isZero();
        assertThat(available(standard)).isZero();
    }

    @Test
    void reserveIsAllOrNothing() {
        TicketStock plenty = createStock(5);
        TicketStock soldOut = createStock(1);
        reservationService.reserve(request(nextOrderId(), item(soldOut, 1)));

        assertThatThrownBy(() -> reservationService.reserve(request(nextOrderId(), item(plenty, 2), item(soldOut, 1)))).isInstanceOf(
            InsufficientStockException.class
        );

        assertThat(available(plenty)).isEqualTo(5);
        assertThat(stockHoldRepository.findAll()).hasSize(1);
    }

    @Test
    void reserveIsIdempotentPerOrder() {
        TicketStock stock = createStock(10);
        long orderId = nextOrderId();

        Reservation first = reservationService.reserve(request(orderId, item(stock, 3)));
        Reservation retry = reservationService.reserve(request(orderId, item(stock, 3)));

        assertThat(retry).isEqualTo(first);
        assertThat(available(stock)).isEqualTo(7);
        assertThatThrownBy(() -> reservationService.reserve(request(orderId, item(stock, 4)))).isInstanceOf(
            ReservationConflictException.class
        );
    }

    @Test
    void duplicateItemsInOneRequestAreMerged() {
        TicketStock stock = createStock(10);

        Reservation reservation = reservationService.reserve(request(nextOrderId(), item(stock, 2), item(stock, 3)));

        assertThat(reservation.lines()).singleElement().extracting(Reservation.Line::quantity).isEqualTo(5);
        assertThat(available(stock)).isEqualTo(5);
    }

    @Test
    void unknownTicketTypeIsRejected() {
        assertThatThrownBy(() -> reservationService.reserve(new ReservationRequest(nextOrderId(), List.of(new ReservationRequest.Item(-1L, 1))))).isInstanceOf(
            UnknownTicketTypeException.class
        );
    }

    @Test
    void expiredHoldsGoBackOnSaleExactlyOnce() {
        TicketStock stock = createStock(10);
        long orderId = nextOrderId();
        reservationService.reserve(request(orderId, item(stock, 4)));

        mutableClock().advance(Duration.ofMinutes(9));
        assertThat(reservationService.expireDueHolds()).isEmpty();

        mutableClock().advance(Duration.ofMinutes(2));
        assertThat(reservationService.expireDueHolds()).singleElement().satisfies(expired -> {
            assertThat(expired.orderId()).isEqualTo(orderId);
            assertThat(expired.quantity()).isEqualTo(4);
        });
        assertThat(reservationService.expireDueHolds()).isEmpty();

        assertThat(available(stock)).isEqualTo(10);
        assertThat(holdsOf(orderId)).allMatch(hold -> hold.getStatus() == HoldStatus.EXPIRED);
        assertThatThrownBy(() -> reservationService.confirm(orderId)).isInstanceOf(ReservationConflictException.class);
    }

    @Test
    void holdPastItsDeadlineCannotBeConfirmedEvenBeforeTheJobRuns() {
        TicketStock stock = createStock(10);
        long orderId = nextOrderId();
        reservationService.reserve(request(orderId, item(stock, 1)));

        mutableClock().advance(Duration.ofMinutes(10));

        assertThatThrownBy(() -> reservationService.confirm(orderId)).isInstanceOf(ReservationConflictException.class);
    }

    @Test
    void confirmedSeatsSurviveReleaseAndExpiry() {
        TicketStock stock = createStock(10);
        long orderId = nextOrderId();
        reservationService.reserve(request(orderId, item(stock, 2)));

        reservationService.confirm(orderId);
        reservationService.confirm(orderId);
        reservationService.release(orderId);
        mutableClock().advance(Duration.ofHours(1));
        reservationService.expireDueHolds();

        assertThat(available(stock)).isEqualTo(8);
        assertThat(holdsOf(orderId)).allMatch(hold -> hold.getStatus() == HoldStatus.CONFIRMED);
    }

    @Test
    void releaseReturnsSeatsOnce() {
        TicketStock stock = createStock(10);
        long orderId = nextOrderId();
        reservationService.reserve(request(orderId, item(stock, 3)));

        reservationService.release(orderId);
        reservationService.release(orderId);

        assertThat(available(stock)).isEqualTo(10);
        assertThat(holdsOf(orderId)).allMatch(hold -> hold.getStatus() == HoldStatus.RELEASED);
    }

    private enum Outcome {
        SUCCESS,
        SOLD_OUT,
    }

    private interface TaskFactory {
        Callable<Outcome> create(int index);
    }

    /** Starts all tasks at the same instant and waits for every one of them. */
    private static List<Outcome> race(int tasks, TaskFactory factory) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(32)) {
            List<Future<Outcome>> futures = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                Callable<Outcome> task = factory.create(i);
                futures.add(
                    executor.submit(() -> {
                        start.await();
                        return task.call();
                    })
                );
            }
            start.countDown();
            List<Outcome> outcomes = new ArrayList<>();
            for (Future<Outcome> future : futures) {
                outcomes.add(future.get(60, TimeUnit.SECONDS));
            }
            return outcomes;
        }
    }

    private Outcome reserve(long orderId, ReservationRequest.Item... items) {
        try {
            reservationService.reserve(request(orderId, items));
            return Outcome.SUCCESS;
        } catch (InsufficientStockException e) {
            return Outcome.SOLD_OUT;
        }
    }

    private TicketStock createStock(int seats) {
        long id = IDS.incrementAndGet();
        return ticketStockRepository.saveAndFlush(
            new TicketStock().ticketTypeId(id).eventId(1L).totalQuantity(seats).availableQuantity(seats)
        );
    }

    private int available(TicketStock stock) {
        return ticketStockRepository.findById(stock.getId()).orElseThrow().getAvailableQuantity();
    }

    private List<StockHold> holdsOf(long orderId) {
        return stockHoldRepository.findAllByOrderId(orderId);
    }

    private MutableClock mutableClock() {
        return (MutableClock) clock;
    }

    private static long nextOrderId() {
        return IDS.incrementAndGet();
    }

    private static ReservationRequest.Item item(TicketStock stock, int quantity) {
        return new ReservationRequest.Item(stock.getTicketTypeId(), quantity);
    }

    private static ReservationRequest request(long orderId, ReservationRequest.Item... items) {
        return new ReservationRequest(orderId, List.of(items));
    }
}
