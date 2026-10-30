package com.ticketrush.order.service.saga;

import com.ticketrush.order.broker.OrderEvent;
import com.ticketrush.order.broker.PaymentResultEvent;
import com.ticketrush.order.client.InventoryClient;
import com.ticketrush.order.client.PaymentClient;
import com.ticketrush.order.domain.Order;
import com.ticketrush.order.domain.enumeration.OrderStatus;
import com.ticketrush.order.repository.OrderRepository;
import com.ticketrush.order.repository.ProcessedEvents;
import com.ticketrush.order.service.checkout.DependencyUnavailableException;
import com.ticketrush.order.service.checkout.OrderNotFoundException;
import com.ticketrush.order.service.checkout.OrderStateException;
import com.ticketrush.order.service.checkout.OrderSummary;
import com.ticketrush.order.service.outbox.Outbox;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Orchestrates an order after its seats are held: payment, then confirmation, with compensations
 * (see docs/adr/0002-saga-orchestration.md).
 *
 * <pre>
 * RESERVED --payment opened--> AWAITING_PAYMENT --paid, seats confirmed--> CONFIRMED
 *                                  |  payment failed / expired: release seats --> FAILED / EXPIRED
 *                                  |  seats expired: cancel payment ------------> EXPIRED
 *                                  |  paid but seats gone: refund --------------> REFUNDED
 *                                  |  buyer cancels: cancel payment, release ---> CANCELLED
 * </pre>
 *
 * The state lives in the order row (optimistic locking), so a restart resumes where it stopped. Steps that call
 * other services run outside any database transaction and are idempotent there, so a redelivered event can safely
 * repeat them; the state change, the processed-event record and the outgoing {@link OrderEvent} then commit
 * together.
 */
@Service
public class OrderSaga {

    private static final Logger LOG = LoggerFactory.getLogger(OrderSaga.class);

    private static final String PAYMENT_RESULTS = "saga-payment-results";

    private static final String HOLD_EXPIRIES = "saga-hold-expiries";

    private static final Set<OrderStatus> OPEN = EnumSet.of(OrderStatus.RESERVED, OrderStatus.AWAITING_PAYMENT);

    private static final Set<OrderStatus> CANCELLABLE = EnumSet.of(OrderStatus.PENDING, OrderStatus.RESERVED, OrderStatus.AWAITING_PAYMENT);

    private static final Set<OrderStatus> SEATS_GONE = EnumSet.of(OrderStatus.EXPIRED, OrderStatus.CANCELLED, OrderStatus.FAILED);

    private static final Set<OrderStatus> FINAL = EnumSet.of(
        OrderStatus.CONFIRMED,
        OrderStatus.CANCELLED,
        OrderStatus.EXPIRED,
        OrderStatus.FAILED,
        OrderStatus.REFUNDED
    );

    private final OrderRepository orderRepository;

    private final InventoryClient inventoryClient;

    private final PaymentClient paymentClient;

    private final ProcessedEvents processedEvents;

    private final Outbox outbox;

    private final TransactionTemplate transactionTemplate;

    private final Clock clock;

    private final MeterRegistry meterRegistry;

    public OrderSaga(
        OrderRepository orderRepository,
        InventoryClient inventoryClient,
        PaymentClient paymentClient,
        ProcessedEvents processedEvents,
        Outbox outbox,
        TransactionTemplate transactionTemplate,
        Clock clock,
        MeterRegistry meterRegistry
    ) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.processedEvents = processedEvents;
        this.outbox = outbox;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.meterRegistry = meterRegistry;
    }

    /**
     * InitiatePayment: opens the payment of a RESERVED order and moves it to AWAITING_PAYMENT.
     * If payment cannot be opened the seats are released and the order FAILS.
     *
     * @throws DependencyUnavailableException if payment did not answer.
     */
    public OrderSummary requestPayment(Long orderId) {
        Order order = orderRepository.findOneWithItemsById(orderId).orElseThrow();
        PaymentClient.Payment payment;
        try {
            payment = paymentClient.create(
                new PaymentClient.CreatePayment(order.getId(), order.getCode(), order.getTotalAmount(), order.getCurrency(), order.getExpiresAt())
            );
        } catch (RestClientException e) {
            LOG.warn("Could not open the payment of order {}: {}", order.getCode(), e.getMessage());
            releaseQuietly(orderId);
            inTransaction(() -> transition(orderId, OPEN, OrderStatus.FAILED, "PAYMENT_UNAVAILABLE"));
            throw new DependencyUnavailableException("Payment did not answer", e);
        }
        return inTransaction(() ->
            transition(orderId, EnumSet.of(OrderStatus.RESERVED), OrderStatus.AWAITING_PAYMENT, null)
                .map(o -> o.paymentUrl(payment.paymentUrl()))
                .map(OrderSummary::of)
                .orElseGet(() -> OrderSummary.of(orderRepository.findOneWithItemsById(orderId).orElseThrow()))
        );
    }

    /**
     * ConfirmOrder or compensate, depending on the payment outcome and on whether the seats are still held.
     */
    public void onPaymentResult(PaymentResultEvent event, UUID eventId) {
        if (alreadyProcessed(PAYMENT_RESULTS, eventId)) {
            return;
        }
        Optional<Order> found = orderRepository.findById(event.orderId());
        if (found.isEmpty()) {
            LOG.warn("Payment result for unknown order {}", event.orderId());
            complete(PAYMENT_RESULTS, eventId, () -> Optional.empty());
            return;
        }
        Long orderId = event.orderId();
        OrderStatus status = found.orElseThrow().getStatus();
        if ("SUCCEEDED".equals(event.status())) {
            if (OPEN.contains(status)) {
                confirmOrRefund(orderId, eventId);
            } else if (SEATS_GONE.contains(status)) {
                // Paid after the order had already ended: there are no seats to give, so give the money back
                refund(orderId);
                complete(PAYMENT_RESULTS, eventId, () -> transition(orderId, SEATS_GONE, OrderStatus.REFUNDED, "PAID_AFTER_" + status));
            } else {
                complete(PAYMENT_RESULTS, eventId, () -> Optional.empty());
            }
            return;
        }
        if (OPEN.contains(status)) {
            release(orderId);
            OrderStatus to = "EXPIRED".equals(event.status()) ? OrderStatus.EXPIRED : OrderStatus.FAILED;
            complete(PAYMENT_RESULTS, eventId, () -> transition(orderId, OPEN, to, "PAYMENT_" + event.status()));
        } else {
            complete(PAYMENT_RESULTS, eventId, () -> Optional.empty());
        }
    }

    /**
     * The seats went back on sale. Abandon the payment and expire the order, unless the buyer already paid: then
     * the payment result that follows refunds it.
     */
    public void onHoldExpired(Long orderId, UUID eventId) {
        if (alreadyProcessed(HOLD_EXPIRIES, eventId)) {
            return;
        }
        Optional<Order> order = orderRepository.findById(orderId);
        if (order.isEmpty() || !OPEN.contains(order.orElseThrow().getStatus())) {
            complete(HOLD_EXPIRIES, eventId, () -> Optional.empty());
            return;
        }
        if (order.orElseThrow().getStatus() == OrderStatus.AWAITING_PAYMENT) {
            try {
                paymentClient.cancel(orderId);
            } catch (HttpClientErrorException.NotFound e) {
                LOG.debug("Order {} had no payment to cancel", orderId);
            } catch (HttpClientErrorException.Conflict e) {
                LOG.info("Seats of order {} expired but it was paid; refunding when the payment result arrives", orderId);
                complete(HOLD_EXPIRIES, eventId, () -> Optional.empty());
                return;
            }
        }
        complete(HOLD_EXPIRIES, eventId, () -> transition(orderId, OPEN, OrderStatus.EXPIRED, "HOLD_EXPIRED"));
    }

    /**
     * Buyer cancels an unpaid order. Idempotent.
     *
     * @throws OrderStateException if the order is already paid or otherwise over.
     */
    public OrderSummary cancel(String userLogin, String code) {
        Order order = orderRepository.findOneWithItemsByCodeAndUserLogin(code, userLogin).orElseThrow(() -> new OrderNotFoundException(code));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return OrderSummary.of(order);
        }
        if (!CANCELLABLE.contains(order.getStatus())) {
            throw new OrderStateException("Order " + code + " is " + order.getStatus() + " and can no longer be cancelled");
        }
        try {
            if (order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
                try {
                    paymentClient.cancel(order.getId());
                } catch (HttpClientErrorException.NotFound e) {
                    LOG.debug("Order {} had no payment to cancel", code);
                } catch (HttpClientErrorException.Conflict e) {
                    throw new OrderStateException("Order " + code + " is already paid");
                }
            }
            release(order.getId());
        } catch (OrderStateException e) {
            throw e;
        } catch (RestClientException e) {
            throw new DependencyUnavailableException("A dependency did not answer", e);
        }
        Long orderId = order.getId();
        return inTransaction(() ->
            transition(orderId, CANCELLABLE, OrderStatus.CANCELLED, "CANCELLED_BY_BUYER")
                .map(OrderSummary::of)
                .orElseGet(() -> OrderSummary.of(orderRepository.findOneWithItemsById(orderId).orElseThrow()))
        );
    }

    private void confirmOrRefund(Long orderId, UUID eventId) {
        try {
            inventoryClient.confirm(orderId);
        } catch (HttpClientErrorException.Conflict e) {
            // Compensation: the money came but the seats are gone
            LOG.info("Order {} was paid but its seats are no longer held; refunding", orderId);
            refund(orderId);
            complete(PAYMENT_RESULTS, eventId, () -> transition(orderId, OPEN, OrderStatus.REFUNDED, "SEATS_EXPIRED"));
            return;
        }
        complete(PAYMENT_RESULTS, eventId, () -> transition(orderId, OPEN, OrderStatus.CONFIRMED, null));
    }

    private void refund(Long orderId) {
        try {
            paymentClient.refund(orderId);
        } catch (HttpClientErrorException.Conflict e) {
            LOG.error("Order {} should be refunded but payment reports no succeeded payment", orderId);
        }
    }

    private void release(Long orderId) {
        try {
            inventoryClient.release(orderId);
        } catch (HttpClientErrorException.Conflict e) {
            LOG.debug("Order {} had no holds to release", orderId);
        }
    }

    private void releaseQuietly(Long orderId) {
        try {
            release(orderId);
        } catch (RestClientException e) {
            // Holds still expire on their own
            LOG.warn("Could not release holds of order {}: {}", orderId, e.getMessage());
        }
    }

    private boolean alreadyProcessed(String consumer, UUID eventId) {
        boolean seen = eventId != null && processedEvents.alreadyProcessed(consumer, eventId);
        if (seen) {
            LOG.debug("Skipping already processed event {} for {}", eventId, consumer);
        }
        return seen;
    }

    /**
     * Records the event as processed and applies the state change in one transaction. A concurrent duplicate that
     * got here first wins; this one then changes nothing.
     */
    private void complete(String consumer, UUID eventId, Supplier<Optional<Order>> change) {
        transactionTemplate.executeWithoutResult(status -> {
            if (eventId == null || processedEvents.firstDelivery(consumer, eventId, clock.instant())) {
                change.get();
            }
        });
    }

    private <T> T inTransaction(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }

    /**
     * Moves the order to {@code to} if it is in one of {@code from}; publishes an {@link OrderEvent} when that is a
     * final state. Must run in a transaction.
     *
     * @return the changed order, or empty if it was not in an expected state (already handled).
     */
    private Optional<Order> transition(Long orderId, Set<OrderStatus> from, OrderStatus to, String reason) {
        Optional<Order> found = orderRepository.findOneWithItemsById(orderId).filter(order -> from.contains(order.getStatus()));
        found.ifPresent(order -> {
            LOG.info("Order {}: {} -> {}{}", order.getCode(), order.getStatus(), to, reason == null ? "" : " (" + reason + ")");
            order.status(to).updatedAt(clock.instant());
            if (reason != null) {
                order.failureReason(reason);
            }
            if (FINAL.contains(to)) {
                meterRegistry.counter("ticketrush.orders", "status", to.name(), "reason", reason == null ? "none" : reason).increment();
                outbox.append(
                    OrderEvent.TOPIC,
                    "Order",
                    order.getId(),
                    "ORDER_" + to.name(),
                    new OrderEvent(
                        order.getId(),
                        order.getCode(),
                        order.getUserLogin(),
                        to.name(),
                        order.getTotalAmount(),
                        order.getCurrency(),
                        order.getFailureReason(),
                        clock.instant()
                    )
                );
            }
        });
        return found;
    }
}
