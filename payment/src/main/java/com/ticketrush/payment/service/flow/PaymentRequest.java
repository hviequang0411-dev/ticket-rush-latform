package com.ticketrush.payment.service.flow;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * What the order saga asks to be paid. {@code orderId} is the idempotency key: asking again while a payment is
 * open returns that payment.
 */
public record PaymentRequest(
    @NotNull Long orderId,
    @NotBlank @Size(max = 32) String orderCode,
    @NotNull @DecimalMin("0") BigDecimal amount,
    @NotBlank @Size(min = 3, max = 3) String currency,
    @NotNull Instant expiresAt
) {}
