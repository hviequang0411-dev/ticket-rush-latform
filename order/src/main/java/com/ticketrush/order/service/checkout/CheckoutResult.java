package com.ticketrush.order.service.checkout;

/**
 * @param replayed true when the Idempotency-Key was seen before and the existing order is returned unchanged.
 */
public record CheckoutResult(OrderSummary order, boolean replayed) {}
