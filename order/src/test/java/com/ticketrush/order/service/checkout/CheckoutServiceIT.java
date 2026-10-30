package com.ticketrush.order.service.checkout;

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
import com.ticketrush.order.client.CatalogClient;
import com.ticketrush.order.client.InventoryClient;
import com.ticketrush.order.client.PaymentClient;
import com.ticketrush.order.domain.Order;
import com.ticketrush.order.domain.enumeration.OrderStatus;
import com.ticketrush.order.repository.OrderItemRepository;
import com.ticketrush.order.repository.OrderRepository;
import com.ticketrush.order.service.saga.OrderSaga;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Real PostgreSQL, catalog and inventory mocked at their HTTP client interfaces.
 */
@IntegrationTest
@TestPropertySource(properties = "spring.datasource.hikari.maximum-pool-size=10")
class CheckoutServiceIT {

    private static final String ALICE = "alice";

    private static final long VIP = 11L;

    private static final long STANDARD = 12L;

    private static final Instant HOLD_EXPIRES = Instant.parse("2031-01-01T10:10:00Z");

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @MockitoBean
    private CatalogClient catalogClient;

    @MockitoBean
    private InventoryClient inventoryClient;

    @MockitoBean
    private PaymentClient paymentClient;

    @Autowired
    private OrderSaga orderSaga;

    @BeforeEach
    void stubCatalog() {
        when(paymentClient.create(any())).thenAnswer(invocation -> {
            PaymentClient.CreatePayment request = invocation.getArgument(0);
            return new PaymentClient.Payment(request.orderId(), "PAYREF", "PENDING", "http://pay/" + request.orderId(), request.amount(), request.currency(), request.expiresAt());
        });
        when(catalogClient.getTicketType(VIP)).thenReturn(ticketType(VIP, "2500000", null, null));
        when(catalogClient.getTicketType(STANDARD)).thenReturn(ticketType(STANDARD, "800000", null, null));
        when(inventoryClient.reserve(any())).thenAnswer(invocation -> {
            InventoryClient.ReserveRequest request = invocation.getArgument(0);
            return new InventoryClient.Reservation(request.orderId(), HOLD_EXPIRES, List.of());
        });
    }

    @AfterEach
    void cleanup() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void reservesSeatsAtCatalogPrices() {
        CheckoutResult result = checkoutService.checkout(ALICE, key(), request(item(VIP, 2), item(STANDARD, 1), item(VIP, 1)));

        assertThat(result.replayed()).isFalse();
        OrderSummary order = result.order();
        assertThat(order.status()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        assertThat(order.paymentUrl()).isEqualTo("http://pay/" + orderOf(order).getId());
        assertThat(order.expiresAt()).isEqualTo(HOLD_EXPIRES);
        assertThat(order.currency()).isEqualTo("VND");
        assertThat(order.totalAmount()).isEqualByComparingTo("8300000");
        assertThat(order.items()).extracting(OrderSummary.Item::ticketTypeId, OrderSummary.Item::quantity).containsExactly(
            org.assertj.core.groups.Tuple.tuple(VIP, 3),
            org.assertj.core.groups.Tuple.tuple(STANDARD, 1)
        );

        ArgumentCaptor<InventoryClient.ReserveRequest> reserved = ArgumentCaptor.forClass(InventoryClient.ReserveRequest.class);
        verify(inventoryClient).reserve(reserved.capture());
        assertThat(reserved.getValue().orderId()).isEqualTo(orderOf(order).getId());
        assertThat(reserved.getValue().items()).containsExactlyInAnyOrder(
            new InventoryClient.ReserveRequest.Item(VIP, 3),
            new InventoryClient.ReserveRequest.Item(STANDARD, 1)
        );
    }

    @Test
    void retryWithSameKeyReturnsTheSameOrder() {
        String key = key();
        CheckoutResult first = checkoutService.checkout(ALICE, key, request(item(VIP, 1)));
        CheckoutResult retry = checkoutService.checkout(ALICE, key, request(item(VIP, 1)));

        assertThat(retry.replayed()).isTrue();
        assertThat(retry.order().code()).isEqualTo(first.order().code());
        assertThat(orderRepository.count()).isEqualTo(1);
        verify(inventoryClient, times(1)).reserve(any());
    }

    @Test
    void concurrentRetriesWithSameKeyCreateOneOrder() throws Exception {
        String key = key();
        CountDownLatch start = new CountDownLatch(1);
        List<Future<CheckoutResult>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 8; i++) {
                futures.add(
                    executor.submit(() -> {
                        start.await();
                        return checkoutService.checkout(ALICE, key, request(item(VIP, 1)));
                    })
                );
            }
            start.countDown();
            List<String> codes = new ArrayList<>();
            for (Future<CheckoutResult> future : futures) {
                codes.add(future.get(60, TimeUnit.SECONDS).order().code());
            }
            assertThat(codes).containsOnly(codes.get(0));
        }
        assertThat(orderRepository.count()).isEqualTo(1);
        verify(inventoryClient, times(1)).reserve(any());
    }

    @Test
    void keyOfAnotherUserIsRejected() {
        String key = key();
        checkoutService.checkout(ALICE, key, request(item(VIP, 1)));

        assertThatThrownBy(() -> checkoutService.checkout("mallory", key, request(item(VIP, 1)))).isInstanceOf(OrderStateException.class);
    }

    @Test
    void soldOutOrderIsKeptAsFailedAndReplaysAsSoldOut() {
        doThrow(httpError(HttpStatus.CONFLICT)).when(inventoryClient).reserve(any());
        String key = key();

        assertThatThrownBy(() -> checkoutService.checkout(ALICE, key, request(item(VIP, 1)))).isInstanceOf(SoldOutException.class);
        assertThatThrownBy(() -> checkoutService.checkout(ALICE, key, request(item(VIP, 1)))).isInstanceOf(SoldOutException.class);

        assertThat(orderRepository.findAll()).singleElement().satisfies(order -> {
            assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
            assertThat(order.getFailureReason()).isEqualTo("SOLD_OUT");
        });
        verify(inventoryClient, times(1)).reserve(any());
    }

    @Test
    void inventoryOutageFailsTheOrderAndReleasesAnyHold() {
        doThrow(new ResourceAccessException("read timed out")).when(inventoryClient).reserve(any());

        assertThatThrownBy(() -> checkoutService.checkout(ALICE, key(), request(item(VIP, 1)))).isInstanceOf(
            DependencyUnavailableException.class
        );

        Order order = orderRepository.findAll().get(0);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        verify(inventoryClient).release(order.getId());
    }

    @Test
    void unknownTicketTypeIsRejectedBeforeAnythingIsWritten() {
        when(catalogClient.getTicketType(99L)).thenThrow(httpError(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> checkoutService.checkout(ALICE, key(), request(item(99L, 1)))).isInstanceOf(CheckoutRejectedException.class);

        assertThat(orderRepository.count()).isZero();
        verify(inventoryClient, never()).reserve(any());
    }

    @Test
    void ticketTypeOutsideItsSaleWindowIsRejected() {
        when(catalogClient.getTicketType(VIP)).thenReturn(ticketType(VIP, "2500000", null, Instant.parse("2020-01-01T00:00:00Z")));

        assertThatThrownBy(() -> checkoutService.checkout(ALICE, key(), request(item(VIP, 1)))).isInstanceOf(CheckoutRejectedException.class);
    }

    @Test
    void expiryOnlyAffectsOpenOrders() {
        OrderSummary reserved = checkoutService.checkout(ALICE, key(), request(item(VIP, 1))).order();
        OrderSummary cancelled = checkoutService.cancel(ALICE, checkoutService.checkout(ALICE, key(), request(item(VIP, 1))).order().code());

        orderSaga.onHoldExpired(orderOf(reserved).getId(), java.util.UUID.randomUUID());
        orderSaga.onHoldExpired(orderOf(reserved).getId(), java.util.UUID.randomUUID());
        orderSaga.onHoldExpired(orderOf(cancelled).getId(), java.util.UUID.randomUUID());

        assertThat(orderOf(reserved).getStatus()).isEqualTo(OrderStatus.EXPIRED);
        assertThat(orderOf(cancelled).getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void cancelReleasesSeatsOnceAndIsIdempotent() {
        OrderSummary order = checkoutService.checkout(ALICE, key(), request(item(VIP, 1))).order();

        assertThat(checkoutService.cancel(ALICE, order.code()).status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(checkoutService.cancel(ALICE, order.code()).status()).isEqualTo(OrderStatus.CANCELLED);

        verify(inventoryClient, times(1)).release(orderOf(order).getId());
        assertThatThrownBy(() -> checkoutService.cancel("mallory", order.code())).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void expiredOrderCannotBeCancelled() {
        OrderSummary order = checkoutService.checkout(ALICE, key(), request(item(VIP, 1))).order();
        orderSaga.onHoldExpired(orderOf(order).getId(), java.util.UUID.randomUUID());

        assertThatThrownBy(() -> checkoutService.cancel(ALICE, order.code())).isInstanceOf(OrderStateException.class);
        verify(inventoryClient, never()).release(anyLong());
    }

    private Order orderOf(OrderSummary summary) {
        return orderRepository.findAll().stream().filter(order -> order.getCode().equals(summary.code())).findFirst().orElseThrow();
    }

    private static CatalogClient.TicketType ticketType(long id, String price, Instant saleStart, Instant saleEnd) {
        return new CatalogClient.TicketType(id, "T" + id, new BigDecimal(price), "VND", saleStart, saleEnd, new CatalogClient.EventRef(1L));
    }

    private static HttpClientErrorException httpError(HttpStatus status) {
        return HttpClientErrorException.create(status, status.getReasonPhrase(), new HttpHeaders(), new byte[0], null);
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }

    private static CheckoutRequest.Item item(long ticketTypeId, int quantity) {
        return new CheckoutRequest.Item(ticketTypeId, quantity);
    }

    private static CheckoutRequest request(CheckoutRequest.Item... items) {
        return new CheckoutRequest(List.of(items));
    }
}
