package com.ticketrush.payment.domain;

import com.ticketrush.payment.domain.enumeration.PaymentProvider;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Every provider callback as received; (provider, eventKey) is unique, so a redelivered webhook is a no-op.
 */
@Entity
@Table(name = "payment_webhook_event")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PaymentWebhookEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private PaymentProvider provider;

    @NotNull
    @Size(max = 128)
    @Column(name = "event_key", length = 128, nullable = false)
    private String eventKey;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @NotNull
    @Column(name = "signature_valid", nullable = false)
    private Boolean signatureValid;

    @NotNull
    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private Payment payment;

    /** Deliveries of this callback, retries included; only the first one is applied. */
    @Column(name = "deliveries", nullable = false)
    private Integer deliveries = 1;

    @Column(name = "last_received_at")
    private Instant lastReceivedAt;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public PaymentWebhookEvent id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PaymentProvider getProvider() {
        return this.provider;
    }

    public PaymentWebhookEvent provider(PaymentProvider provider) {
        this.setProvider(provider);
        return this;
    }

    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }

    public String getEventKey() {
        return this.eventKey;
    }

    public PaymentWebhookEvent eventKey(String eventKey) {
        this.setEventKey(eventKey);
        return this;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getPayload() {
        return this.payload;
    }

    public PaymentWebhookEvent payload(String payload) {
        this.setPayload(payload);
        return this;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Boolean getSignatureValid() {
        return this.signatureValid;
    }

    public PaymentWebhookEvent signatureValid(Boolean signatureValid) {
        this.setSignatureValid(signatureValid);
        return this;
    }

    public void setSignatureValid(Boolean signatureValid) {
        this.signatureValid = signatureValid;
    }

    public Instant getReceivedAt() {
        return this.receivedAt;
    }

    public PaymentWebhookEvent receivedAt(Instant receivedAt) {
        this.setReceivedAt(receivedAt);
        return this;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getProcessedAt() {
        return this.processedAt;
    }

    public PaymentWebhookEvent processedAt(Instant processedAt) {
        this.setProcessedAt(processedAt);
        return this;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public Payment getPayment() {
        return this.payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public PaymentWebhookEvent payment(Payment payment) {
        this.setPayment(payment);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    public Integer getDeliveries() {
        return deliveries;
    }

    public Instant getLastReceivedAt() {
        return lastReceivedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaymentWebhookEvent)) {
            return false;
        }
        return getId() != null && getId().equals(((PaymentWebhookEvent) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PaymentWebhookEvent{" +
            "id=" + getId() +
            ", provider='" + getProvider() + "'" +
            ", eventKey='" + getEventKey() + "'" +
            ", payload='" + getPayload() + "'" +
            ", signatureValid='" + getSignatureValid() + "'" +
            ", receivedAt='" + getReceivedAt() + "'" +
            ", processedAt='" + getProcessedAt() + "'" +
            "}";
    }
}
