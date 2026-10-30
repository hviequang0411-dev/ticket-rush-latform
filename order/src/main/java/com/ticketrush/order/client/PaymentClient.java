package com.ticketrush.order.client;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Payments in the payment service. All calls are idempotent per order.
 * <ul>
 *   <li>create: 409 if the order was already paid</li>
 *   <li>cancel: 404 if the order has no payment, 409 if it was paid meanwhile</li>
 *   <li>refund: 409 if the order has no succeeded payment</li>
 * </ul>
 */
@HttpExchange("/api/internal/payments")
public interface PaymentClient {
    @PostExchange
    Payment create(@RequestBody CreatePayment request);

    @PostExchange("/{orderId}/cancel")
    Payment cancel(@PathVariable Long orderId);

    @PostExchange("/{orderId}/refund")
    Payment refund(@PathVariable Long orderId);

    record CreatePayment(Long orderId, String orderCode, BigDecimal amount, String currency, Instant expiresAt) {}

    record Payment(Long orderId, String providerRef, String status, String paymentUrl, BigDecimal amount, String currency, Instant expiresAt) {}
}
