package com.ticketrush.inventory.service.reservation;

import com.ticketrush.inventory.config.ApplicationProperties;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Puts expired holds back on sale. Safe to run on every instance at once (see
 * {@link com.ticketrush.inventory.repository.StockHoldRepository#lockExpiredHolds}).
 */
@Component
@ConditionalOnProperty(prefix = "application.reservation", name = "expiry-job-enabled", havingValue = "true", matchIfMissing = true)
public class HoldExpiryJob {

    private final ReservationService reservationService;

    private final int batchSize;

    public HoldExpiryJob(ReservationService reservationService, ApplicationProperties applicationProperties) {
        this.reservationService = reservationService;
        this.batchSize = applicationProperties.getReservation().getExpiryBatchSize();
    }

    @Scheduled(fixedDelayString = "${application.reservation.expiry-scan-interval:PT5S}")
    public void expireDueHolds() {
        List<ExpiredHold> batch;
        do {
            // Each batch is its own transaction; the expiry events go through the outbox in that transaction
            batch = reservationService.expireDueHolds();
        } while (batch.size() == batchSize);
    }
}
