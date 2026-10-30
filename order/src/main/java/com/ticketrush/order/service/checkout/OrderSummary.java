package com.ticketrush.order.service.checkout;

import com.ticketrush.order.domain.Order;
import com.ticketrush.order.domain.OrderItem;
import com.ticketrush.order.domain.enumeration.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

/**
 * An order as its buyer sees it.
 */
public record OrderSummary(
    String code,
    OrderStatus status,
    BigDecimal totalAmount,
    String currency,
    Instant createdAt,
    Instant expiresAt,
    String failureReason,
    String paymentUrl,
    List<Item> items
) {
    public record Item(Long eventId, Long ticketTypeId, int quantity, BigDecimal unitPrice) {}

    public static OrderSummary of(Order order) {
        List<Item> items = order
            .getItemses()
            .stream()
            .sorted(Comparator.comparing(OrderItem::getTicketTypeId))
            .map(item -> new Item(item.getEventId(), item.getTicketTypeId(), item.getQuantity(), item.getUnitPrice()))
            .toList();
        return new OrderSummary(
            order.getCode(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getCurrency(),
            order.getCreatedAt(),
            order.getExpiresAt(),
            order.getFailureReason(),
            order.getPaymentUrl(),
            items
        );
    }
}
