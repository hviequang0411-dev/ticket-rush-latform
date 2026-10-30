package com.ticketrush.payment.service.flow;

import com.ticketrush.payment.domain.Payment;
import com.ticketrush.payment.domain.enumeration.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentView(
    Long orderId,
    String providerRef,
    PaymentStatus status,
    String paymentUrl,
    BigDecimal amount,
    String currency,
    Instant expiresAt,
    String failureReason
) {
    static PaymentView of(Payment payment) {
        return new PaymentView(
            payment.getOrderId(),
            payment.getProviderRef(),
            payment.getStatus(),
            payment.getPaymentUrl(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getExpiresAt(),
            payment.getFailureReason()
        );
    }
}
