package com.ticketrush.payment.service.dto;

import com.ticketrush.payment.domain.enumeration.PaymentProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.ticketrush.payment.domain.PaymentWebhookEvent} entity.
 */
@Schema(description = "Every provider callback as received; (provider, eventKey) is unique, so a redelivered webhook is a no-op.")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PaymentWebhookEventDTO implements Serializable {

    private Long id;

    @NotNull
    private PaymentProvider provider;

    @NotNull
    @Size(max = 128)
    private String eventKey;

    @Lob
    private String payload;

    @NotNull
    private Boolean signatureValid;

    @NotNull
    private Instant receivedAt;

    private Instant processedAt;

    private PaymentDTO payment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PaymentProvider getProvider() {
        return provider;
    }

    public void setProvider(PaymentProvider provider) {
        this.provider = provider;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Boolean getSignatureValid() {
        return signatureValid;
    }

    public void setSignatureValid(Boolean signatureValid) {
        this.signatureValid = signatureValid;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(Instant processedAt) {
        this.processedAt = processedAt;
    }

    public PaymentDTO getPayment() {
        return payment;
    }

    public void setPayment(PaymentDTO payment) {
        this.payment = payment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaymentWebhookEventDTO)) {
            return false;
        }

        PaymentWebhookEventDTO paymentWebhookEventDTO = (PaymentWebhookEventDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, paymentWebhookEventDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "PaymentWebhookEventDTO{" +
            "id=" + getId() +
            ", provider='" + getProvider() + "'" +
            ", eventKey='" + getEventKey() + "'" +
            ", payload='" + getPayload() + "'" +
            ", signatureValid='" + getSignatureValid() + "'" +
            ", receivedAt='" + getReceivedAt() + "'" +
            ", processedAt='" + getProcessedAt() + "'" +
            ", payment=" + getPayment() +
            "}";
    }
}
