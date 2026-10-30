package com.ticketrush.payment.service.flow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Expires payments nobody completed in time. Safe on every instance at once (SKIP LOCKED).
 */
@Component
public class PaymentExpiryJob {

    private final PaymentFlowService paymentFlowService;

    private final boolean enabled;

    private final int batchSize;

    public PaymentExpiryJob(
        PaymentFlowService paymentFlowService,
        @Value("${ticketrush.payment.expiry-job-enabled:true}") boolean enabled,
        @Value("${ticketrush.payment.expiry-batch-size:100}") int batchSize
    ) {
        this.paymentFlowService = paymentFlowService;
        this.enabled = enabled;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${ticketrush.payment.expiry-scan-interval:PT5S}")
    public void expireDue() {
        if (!enabled) {
            return;
        }
        int expired;
        do {
            expired = paymentFlowService.expireDue();
        } while (expired == batchSize);
    }
}
