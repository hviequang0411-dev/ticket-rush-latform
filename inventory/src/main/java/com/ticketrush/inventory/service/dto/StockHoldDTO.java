package com.ticketrush.inventory.service.dto;

import com.ticketrush.inventory.domain.enumeration.HoldStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.ticketrush.inventory.domain.StockHold} entity.
 */
@Schema(description = "Seats held for an order for 10 minutes; one row per (order, ticket type).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class StockHoldDTO implements Serializable {

    private Long id;

    @NotNull
    private Long orderId;

    @NotNull
    @Min(value = 1)
    private Integer quantity;

    @NotNull
    private HoldStatus status;

    @NotNull
    private Instant expiresAt;

    @NotNull
    private Instant createdAt;

    @NotNull
    private TicketStockDTO stock;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public HoldStatus getStatus() {
        return status;
    }

    public void setStatus(HoldStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public TicketStockDTO getStock() {
        return stock;
    }

    public void setStock(TicketStockDTO stock) {
        this.stock = stock;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StockHoldDTO)) {
            return false;
        }

        StockHoldDTO stockHoldDTO = (StockHoldDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, stockHoldDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "StockHoldDTO{" +
            "id=" + getId() +
            ", orderId=" + getOrderId() +
            ", quantity=" + getQuantity() +
            ", status='" + getStatus() + "'" +
            ", expiresAt='" + getExpiresAt() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            ", stock=" + getStock() +
            "}";
    }
}
