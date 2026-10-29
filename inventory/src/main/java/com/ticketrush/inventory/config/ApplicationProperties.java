package com.ticketrush.inventory.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties specific to Inventory.
 * <p>
 * Properties are configured in the {@code application.yml} file.
 * See {@link tech.jhipster.config.JHipsterProperties} for a good example.
 */
@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private final Liquibase liquibase = new Liquibase();

    private final Reservation reservation = new Reservation();

    // jhipster-needle-application-properties-property

    public Liquibase getLiquibase() {
        return liquibase;
    }

    public Reservation getReservation() {
        return reservation;
    }

    // jhipster-needle-application-properties-property-getter

    public static class Liquibase {

        private Boolean asyncStart = true;

        public Boolean getAsyncStart() {
            return asyncStart;
        }

        public void setAsyncStart(Boolean asyncStart) {
            this.asyncStart = asyncStart;
        }
    }

    public static class Reservation {

        /** How long seats stay held for an order before they go back on sale. */
        private Duration holdTtl = Duration.ofMinutes(10);

        /** Maximum number of expired holds released per transaction by the expiry job. */
        private int expiryBatchSize = 100;

        /** Turns the expiry job off, for tests that drive expiry themselves. */
        private boolean expiryJobEnabled = true;

        /** Pause between two expiry scans. */
        private Duration expiryScanInterval = Duration.ofSeconds(5);

        public Duration getHoldTtl() {
            return holdTtl;
        }

        public void setHoldTtl(Duration holdTtl) {
            this.holdTtl = holdTtl;
        }

        public int getExpiryBatchSize() {
            return expiryBatchSize;
        }

        public void setExpiryBatchSize(int expiryBatchSize) {
            this.expiryBatchSize = expiryBatchSize;
        }

        public boolean isExpiryJobEnabled() {
            return expiryJobEnabled;
        }

        public void setExpiryJobEnabled(boolean expiryJobEnabled) {
            this.expiryJobEnabled = expiryJobEnabled;
        }

        public Duration getExpiryScanInterval() {
            return expiryScanInterval;
        }

        public void setExpiryScanInterval(Duration expiryScanInterval) {
            this.expiryScanInterval = expiryScanInterval;
        }
    }

    // jhipster-needle-application-properties-property-class
}
