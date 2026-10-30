package com.ticketrush.order.service.checkout;

import com.ticketrush.order.client.CatalogClient;
import com.ticketrush.order.client.InventoryClient;
import com.ticketrush.order.domain.Order;
import com.ticketrush.order.domain.OrderItem;
import com.ticketrush.order.domain.enumeration.OrderStatus;
import com.ticketrush.order.repository.OrderItemRepository;
import com.ticketrush.order.repository.OrderRepository;
import com.ticketrush.order.service.saga.OrderSaga;
import java.math.BigDecimal;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

/**
 * Checkout: price from catalog, hold seats in inventory, then hand the order to {@link OrderSaga} to open its
 * payment. Everything after that (confirmation, expiry, refunds, cancellation) is the saga's.
 * <p>
 * No database transaction is open while calling other services; each step that writes commits on its own. A
 * failed reserve marks the order FAILED and releases whatever inventory may have held.
 */
@Service
public class CheckoutService {

    private static final Logger LOG = LoggerFactory.getLogger(CheckoutService.class);

    private final OrderRepository orderRepository;

    private final OrderItemRepository orderItemRepository;

    private final CatalogClient catalogClient;

    private final InventoryClient inventoryClient;

    private final OrderCodeGenerator orderCodeGenerator;

    private final TransactionTemplate transactionTemplate;

    private final Clock clock;

    private final OrderSaga orderSaga;

    private final MeterRegistry meterRegistry;

    public CheckoutService(
        OrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        CatalogClient catalogClient,
        InventoryClient inventoryClient,
        OrderCodeGenerator orderCodeGenerator,
        TransactionTemplate transactionTemplate,
        Clock clock,
        OrderSaga orderSaga,
        MeterRegistry meterRegistry
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.catalogClient = catalogClient;
        this.inventoryClient = inventoryClient;
        this.orderCodeGenerator = orderCodeGenerator;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
        this.orderSaga = orderSaga;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Places an order and holds its seats. Repeating a call with the same {@code idempotencyKey} returns the order
     * created by the first call instead of placing another one.
     *
     * @throws SoldOutException if any ticket type has too few seats left.
     * @throws CheckoutRejectedException if the request can never succeed (unknown ticket type, sales closed, ...).
     * @throws DependencyUnavailableException if catalog or inventory did not answer; retry with the same key.
     */
    public CheckoutResult checkout(String userLogin, String idempotencyKey, CheckoutRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "error";
        try {
            CheckoutResult result = placeOrder(userLogin, idempotencyKey, request);
            outcome = result.replayed() ? "replayed" : "accepted";
            return result;
        } catch (SoldOutException e) {
            outcome = "sold_out";
            throw e;
        } catch (CheckoutRejectedException e) {
            outcome = "rejected";
            throw e;
        } catch (DependencyUnavailableException e) {
            outcome = "dependency_unavailable";
            throw e;
        } catch (OrderStateException e) {
            outcome = "conflict";
            throw e;
        } finally {
            sample.stop(Timer.builder("ticketrush.checkout").description("Checkout requests by outcome").tag("outcome", outcome).register(meterRegistry));
        }
    }

    private CheckoutResult placeOrder(String userLogin, String idempotencyKey, CheckoutRequest request) {
        LOG.debug("Request to checkout for {} with key {} : {}", userLogin, idempotencyKey, request);
        Optional<Order> existing = orderRepository.findOneWithItemsByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.orElseThrow(), userLogin);
        }

        Map<Long, Integer> quantities = request
            .items()
            .stream()
            .collect(Collectors.toMap(CheckoutRequest.Item::ticketTypeId, CheckoutRequest.Item::quantity, Integer::sum, TreeMap::new));
        Instant now = clock.instant();
        List<CatalogClient.TicketType> ticketTypes = quantities.keySet().stream().map(this::ticketType).toList();
        validate(ticketTypes, now);

        Order order;
        try {
            order = transactionTemplate.execute(status -> createPendingOrder(userLogin, idempotencyKey, ticketTypes, quantities, now));
        } catch (DataIntegrityViolationException e) {
            // A concurrent request with the same key won the insert
            return orderRepository
                .findOneWithItemsByIdempotencyKey(idempotencyKey)
                .map(winner -> replay(winner, userLogin))
                .orElseThrow(() -> e);
        }

        InventoryClient.Reservation reservation;
        try {
            reservation = inventoryClient.reserve(reserveRequest(order));
        } catch (HttpClientErrorException.Conflict e) {
            fail(order.getId(), "SOLD_OUT");
            throw new SoldOutException(order.getCode());
        } catch (HttpClientErrorException.BadRequest e) {
            fail(order.getId(), "UNKNOWN_TICKET_TYPE");
            throw new CheckoutRejectedException("Some ticket types are not on sale");
        } catch (RestClientException e) {
            // We don't know whether inventory held the seats; releasing is idempotent, so make sure they are not held
            fail(order.getId(), "INVENTORY_UNAVAILABLE");
            releaseQuietly(order.getId());
            throw new DependencyUnavailableException("Inventory did not answer", e);
        }
        update(order.getId(), o -> o.status(OrderStatus.RESERVED).expiresAt(reservation.expiresAt()));
        return new CheckoutResult(orderSaga.requestPayment(order.getId()), false);
    }

    /**
     * Cancels an order that is not paid yet and puts its seats back on sale. Idempotent.
     */
    public OrderSummary cancel(String userLogin, String code) {
        return orderSaga.cancel(userLogin, code);
    }

    public Page<OrderSummary> findMine(String userLogin, Pageable pageable) {
        return orderRepository
            .findAllByUserLoginOrderByCreatedAtDesc(userLogin, pageable)
            .map(order -> orderRepository.findOneWithItemsById(order.getId()).map(OrderSummary::of).orElseThrow());
    }

    public Optional<OrderSummary> findMine(String userLogin, String code) {
        return orderRepository.findOneWithItemsByCodeAndUserLogin(code, userLogin).map(OrderSummary::of);
    }

    private Order createPendingOrder(
        String userLogin,
        String idempotencyKey,
        List<CatalogClient.TicketType> ticketTypes,
        Map<Long, Integer> quantities,
        Instant now
    ) {
        BigDecimal total = ticketTypes
            .stream()
            .map(type -> type.price().multiply(BigDecimal.valueOf(quantities.get(type.id()))))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Order order = orderRepository.save(
            new Order()
                .code(orderCodeGenerator.next())
                .userLogin(userLogin)
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .currency(ticketTypes.get(0).currency())
                .idempotencyKey(idempotencyKey)
                .createdAt(now)
        );
        for (CatalogClient.TicketType type : ticketTypes) {
            OrderItem item = new OrderItem()
                .eventId(type.eventId())
                .ticketTypeId(type.id())
                .quantity(quantities.get(type.id()))
                .unitPrice(type.price());
            order.addItems(item);
            orderItemRepository.save(item);
        }
        orderRepository.flush();
        return order;
    }

    private CatalogClient.TicketType ticketType(Long id) {
        try {
            return catalogClient.getTicketType(id);
        } catch (HttpClientErrorException.NotFound e) {
            throw new CheckoutRejectedException("Unknown ticket type " + id);
        } catch (RestClientException e) {
            throw new DependencyUnavailableException("Catalog did not answer", e);
        }
    }

    private static void validate(List<CatalogClient.TicketType> ticketTypes, Instant now) {
        if (ticketTypes.stream().map(CatalogClient.TicketType::currency).distinct().count() > 1) {
            throw new CheckoutRejectedException("All tickets of an order must be in the same currency");
        }
        for (CatalogClient.TicketType type : ticketTypes) {
            boolean notStarted = type.saleStart() != null && now.isBefore(type.saleStart());
            boolean ended = type.saleEnd() != null && !now.isBefore(type.saleEnd());
            if (notStarted || ended) {
                throw new CheckoutRejectedException("Ticket type " + type.id() + " is not on sale");
            }
        }
    }

    private static InventoryClient.ReserveRequest reserveRequest(Order order) {
        List<InventoryClient.ReserveRequest.Item> items = order
            .getItemses()
            .stream()
            .map(item -> new InventoryClient.ReserveRequest.Item(item.getTicketTypeId(), item.getQuantity()))
            .toList();
        return new InventoryClient.ReserveRequest(order.getId(), items);
    }

    private CheckoutResult replay(Order order, String userLogin) {
        if (!order.getUserLogin().equals(userLogin)) {
            throw new OrderStateException("Idempotency-Key already used");
        }
        LOG.debug("Replaying order {} for idempotency key {}", order.getCode(), order.getIdempotencyKey());
        if ("SOLD_OUT".equals(order.getFailureReason())) {
            throw new SoldOutException(order.getCode());
        }
        return new CheckoutResult(OrderSummary.of(order), true);
    }

    private void fail(Long orderId, String reason) {
        update(orderId, order -> order.status(OrderStatus.FAILED).failureReason(reason));
    }

    private void releaseQuietly(Long orderId) {
        try {
            inventoryClient.release(orderId);
        } catch (HttpClientErrorException.Conflict e) {
            LOG.debug("Order {} had no holds to release", orderId);
        } catch (RestClientException e) {
            // Holds that were taken will still expire on their own
            LOG.warn("Could not release holds of order {}: {}", orderId, e.getMessage());
        }
    }

    private OrderSummary update(Long orderId, UnaryOperator<Order> change) {
        return transactionTemplate.execute(status -> {
            Order order = orderRepository.findOneWithItemsById(orderId).orElseThrow();
            change.apply(order).updatedAt(clock.instant());
            return OrderSummary.of(order);
        });
    }
}
