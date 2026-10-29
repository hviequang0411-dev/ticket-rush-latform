package com.ticketrush.inventory.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link com.ticketrush.inventory.domain.TicketStock} entity.
 */
@Schema(description = "Live stock per ticket type. Decremented only by a conditional UPDATE (see ADR 0003).")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TicketStockDTO implements Serializable {

    private Long id;

    @NotNull
    private Long ticketTypeId;

    @NotNull
    private Long eventId;

    @NotNull
    @Min(value = 0)
    private Integer totalQuantity;

    @NotNull
    @Min(value = 0)
    private Integer availableQuantity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketTypeId() {
        return ticketTypeId;
    }

    public void setTicketTypeId(Long ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TicketStockDTO)) {
            return false;
        }

        TicketStockDTO ticketStockDTO = (TicketStockDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, ticketStockDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TicketStockDTO{" +
            "id=" + getId() +
            ", ticketTypeId=" + getTicketTypeId() +
            ", eventId=" + getEventId() +
            ", totalQuantity=" + getTotalQuantity() +
            ", availableQuantity=" + getAvailableQuantity() +
            "}";
    }
}
