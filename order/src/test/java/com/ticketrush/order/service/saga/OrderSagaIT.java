package com.ticketrush.order.service.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ticketrush.order.IntegrationTest;
import com.ticketrush.order.broker.HoldExpiredEvent;
import com.ticketrush.order.broker.PaymentResultEvent;
import com.ticketrush.order.client.CatalogClient;
import com.ticketrush.order.client.InventoryClient;
import com.ticketrush.order.client.PaymentClient;
import com.ticketrush.order.domain.Order;
import com.ticketrush.order.domain.OutboxEvent;
import com.ticketrush.order.domain.enumeration.OrderStatus;
import com.ticketrush.order.repository.OrderItemRepository;
import com.ticketrush.order.repository.OrderRepository;
import com.ticketrush.order.repository.OutboxEventRepository;
import com.ticketrush.order.service.checkout.CheckoutRequest;
import com.ticketrush.order.service.checkout.CheckoutService;
import com.ticketrush.order.service.checkout.DependencyUnavailableException;
import com.ticketrush.order.service.checkout.OrderStateException;
import com.ticketrush.order.service.checkout.OrderSummary;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * The saga against real PostgreSQL, fed through the Kafka test binder; inventory, catalog and payment are mocked at
 * their HTTP client interfaces.
 */
@IntegrationTest
@ImportAutoConfiguration(TestChannelBinderConfiguration.class)
// The Kafka binder is on the classpath too; route messages to the in-memory test binder
@TestPropertySource(properties = "spring.cloud.stream.default-binder=integration")
class OrderSagaIT {

    private static final long TICKET_TYPE = 11L;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private OrderSaga orderSaga;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private InputDestination input;

    @MockitoBean
    private CatalogClient catalogClient;

    @MockitoBean
    private InventoryClient inventoryClient;

    @MockitoBean
    private PaymentClient paymentClient;

    @BeforeEach
    void stubDependencies() {
        when(catalogClient.getTicketType(TICKET_TYPE)).thenReturn(
            new CatalogClient.TicketType(TICKET_TYPE, "VIP", new BigDecimal("2500000"), "VND", null, null, new CatalogClient.EventRef(1L))
        );
        when(inventoryClient.reserve(any())).thenAnswer(invocation ->
            new InventoryClient.Reservation(((InventoryClient.ReserveRequest) invocation.getArgument(0)).orderId(), Instant.now().plusSeconds(600), List.of())
        );
        when(paymentClient.create(any())).thenAnswer(invocation -> {
            PaymentClient.CreatePayment request = invocation.getArgument(0);
            return new PaymentClient.Payment(request.orderId(), "PAYREF", "PENDING", "http://pay/x", request.amount(), request.currency(), request.expiresAt());
        });
    }

    @AfterEach
    void cleanup() {
        outboxEventRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void paidOrderIsConfirmedOnceEvenIfTheResultIsRedelivered() {
        Order order = checkout();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);

        UUID eventId = UUID.randomUUID();
        sendPaymentResult(order, "SUCCEEDED", false, eventId);
        sendPaymentResult(order, "SUCCEEDED", false, eventId);

        assertThat(reload(order).getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(inventoryClient, times(1)).confirm(order.getId());
        assertThat(orderEvents(order)).containsExactly("ORDER_CONFIRMED");
    }

    @Test
    void paidButSeatsAlreadyGoneIsRefunded() {
        Order order = checkout();
        doThrow(httpError(HttpStatus.CONFLICT)).when(inventoryClient).confirm(order.getId());

        sendPaymentResult(order, "SUCCEEDED", false, UUID.randomUUID());

        assertThat(reload(order).getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(reload(order).getFailureReason()).isEqualTo("SEATS_EXPIRED");
        verify(paymentClient).refund(order.getId());
        assertThat(orderEvents(order)).containsExactly("ORDER_REFUNDED");
    }

    @Test
    void paymentArrivingAfterTheOrderExpiredIsRefunded() {
        Order order = checkout();
        sendHoldExpired(order);
        assertThat(reload(order).getStatus()).isEqualTo(OrderStatus.EXPIRED);
        verify(paymentClient).cancel(order.getId());

        sendPaymentResult(order, "SUCCEEDED", true, UUID.randomUUID());

        assertThat(reload(order).getStatus()).isEqualTo(OrderStatus.REFUNDED);
        assertThat(reload(order).getFailureReason()).isEqualTo("PAID_AFTER_EXPIRED");
        verify(inventoryClient, never()).confirm(anyLong());
        assertThat(orderEvents(order)).containsExactly("ORDER_EXPIRED", "ORDER_REFUNDED");
    }

    @Test
    void seatsExpiringWhileThePaymentIsBeingCapturedEndInARefund() {
        Order order = checkout();
        doThrow(httpError(HttpStatus.CONFLICT)).when(paymentClient).cancel(order.getId());

        sendHoldExpired(order);
        assertThat(reload(order).getStatus()).as("paid: wait for the payment result").isEqualTo(OrderStatus.AWAITING_PAYMENT);

        doThrow(httpError(HttpStatus.CONFLICT)).when(inventoryClient).confirm(order.getId());
        sendPaymentResult(order, "SUCCEEDED", false, UUID.randomUUID());

        assertThat(reload(order).getStatus()).isEqualTo(OrderStatus.REFUNDED);
        verify(paymentClient).refund(order.getId());
    }

    @Test
    void failedPaymentReleasesTheSeats() {
        Order order = checkout();

        sendPaymentResult(order, "FAILED", false, UUID.randomUUID());

        assertThat(reload(order).getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(reload(order).getFailureReason()).isEqualTo("PAYMENT_FAILED");
        verify(inventoryClient).release(order.getId());
        verify(paymentClient, never()).refund(anyLong());
    }

    @Test
    void paymentServiceDownAtCheckoutReleasesTheSeats() {
        doThrow(new ResourceAccessException("connect timed out")).when(paymentClient).create(any());

        assertThatThrownBy(() -> checkoutService.checkout("alice", UUID.randomUUID().toString(), request())).isInstanceOf(
            DependencyUnavailableException.class
        );

        Order order = orderRepository.findAll().get(0);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(order.getFailureReason()).isEqualTo("PAYMENT_UNAVAILABLE");
        verify(inventoryClient).release(order.getId());
    }

    @Test
    void cancellingCancelsThePaymentAndReleasesTheSeats() {
        Order order = checkout();

        OrderSummary cancelled = checkoutService.cancel("alice", order.getCode());

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(paymentClient).cancel(order.getId());
        verify(inventoryClient).release(order.getId());
        assertThat(orderEvents(order)).containsExactly("ORDER_CANCELLED");
    }

    @Test
    void aPaidOrderCannotBeCancelled() {
        Order order = checkout();
        doThrow(httpError(HttpStatus.CONFLICT)).when(paymentClient).cancel(order.getId());

        assertThatThrownBy(() -> checkoutService.cancel("alice", order.getCode())).isInstanceOf(OrderStateException.class);

        assertThat(reload(order).getStatus()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        verify(inventoryClient, never()).release(anyLong());
    }

    private Order checkout() {
        String code = checkoutService.checkout("alice", UUID.randomUUID().toString(), request()).order().code();
        return orderRepository.findAll().stream().filter(o -> o.getCode().equals(code)).findFirst().orElseThrow();
    }

    private Order reload(Order order) {
        return orderRepository.findById(order.getId()).orElseThrow();
    }

    private List<String> orderEvents(Order order) {
        return outboxEventRepository
            .findAll()
            .stream()
            .filter(event -> event.getAggregateId().equals(order.getId().toString()))
            .sorted(java.util.Comparator.comparing(OutboxEvent::getCreatedAt))
            .map(OutboxEvent::getEventType)
            .toList();
    }

    private void sendPaymentResult(Order order, String status, boolean late, UUID eventId) {
        input.send(
            MessageBuilder.withPayload(
                new PaymentResultEvent(order.getId(), order.getCode(), "PAYREF", status, order.getTotalAmount(), "VND", late, null, Instant.now())
            )
                .setHeader("eventId", eventId.toString())
                .build(),
            PaymentResultEvent.TOPIC
        );
    }

    private void sendHoldExpired(Order order) {
        input.send(
            MessageBuilder.withPayload(new HoldExpiredEvent(order.getId(), Instant.now())).setHeader("eventId", UUID.randomUUID().toString()).build(),
            "inventory.hold-expired.v1"
        );
    }

    private static CheckoutRequest request() {
        return new CheckoutRequest(List.of(new CheckoutRequest.Item(TICKET_TYPE, 2)));
    }

    private static HttpClientErrorException httpError(HttpStatus status) {
        return HttpClientErrorException.create(status, status.getReasonPhrase(), new HttpHeaders(), new byte[0], null);
    }
}
