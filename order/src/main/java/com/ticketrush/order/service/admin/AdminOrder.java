package com.ticketrush.order.service.admin;

import com.ticketrush.order.domain.Order;
import com.ticketrush.order.domain.enumeration.OrderStatus;
import com.ticketrush.order.service.checkout.OrderSummary;
import com.ticketrush.order.service.ticket.TicketView;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * An order as the organiser sees it: whose it is and, once confirmed, its tickets.
 */
public record AdminOrder(
    String code,
    String userLogin,
    OrderStatus status,
    BigDecimal totalAmount,
    String currency,
    Instant createdAt,
    Instant updatedAt,
    Instant expiresAt,
    String failureReason,
    List<OrderSummary.Item> items,
    List<TicketView> tickets
) {
    static AdminOrder of(Order order, List<TicketView> tickets) {
        OrderSummary summary = OrderSummary.of(order);
        return new AdminOrder(
            order.getCode(),
            order.getUserLogin(),
            order.getStatus(),
            order.getTotalAmount(),
            order.getCurrency(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            order.getExpiresAt(),
            order.getFailureReason(),
            summary.items(),
            tickets
        );
    }
}
