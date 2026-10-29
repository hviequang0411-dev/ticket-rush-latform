package com.ticketrush.catalog.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO for the {@link com.ticketrush.catalog.domain.TicketType} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TicketTypeDTO implements Serializable {

    private Long id;

    @NotNull
    @Size(max = 100)
    private String name;

    @NotNull
    @DecimalMin(value = "0")
    private BigDecimal price;

    @NotNull
    @Size(min = 3, max = 3)
    private String currency;

    @NotNull
    @Min(value = 1)
    private Integer totalQuantity;

    private Instant saleStart;

    private Instant saleEnd;

    @NotNull
    private EventDTO event;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Instant getSaleStart() {
        return saleStart;
    }

    public void setSaleStart(Instant saleStart) {
        this.saleStart = saleStart;
    }

    public Instant getSaleEnd() {
        return saleEnd;
    }

    public void setSaleEnd(Instant saleEnd) {
        this.saleEnd = saleEnd;
    }

    public EventDTO getEvent() {
        return event;
    }

    public void setEvent(EventDTO event) {
        this.event = event;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TicketTypeDTO)) {
            return false;
        }

        TicketTypeDTO ticketTypeDTO = (TicketTypeDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, ticketTypeDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TicketTypeDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", price=" + getPrice() +
            ", currency='" + getCurrency() + "'" +
            ", totalQuantity=" + getTotalQuantity() +
            ", saleStart='" + getSaleStart() + "'" +
            ", saleEnd='" + getSaleEnd() + "'" +
            ", event=" + getEvent() +
            "}";
    }
}
