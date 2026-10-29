package com.ticketrush.inventory.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;

/**
 * Live stock per ticket type. Decremented only by a conditional UPDATE (see ADR 0003).
 */
@Entity
@Table(name = "ticket_stock")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TicketStock implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "ticket_type_id", nullable = false, unique = true)
    private Long ticketTypeId;

    @NotNull
    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @NotNull
    @Min(value = 0)
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @NotNull
    @Min(value = 0)
    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public TicketStock id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketTypeId() {
        return this.ticketTypeId;
    }

    public TicketStock ticketTypeId(Long ticketTypeId) {
        this.setTicketTypeId(ticketTypeId);
        return this;
    }

    public void setTicketTypeId(Long ticketTypeId) {
        this.ticketTypeId = ticketTypeId;
    }

    public Long getEventId() {
        return this.eventId;
    }

    public TicketStock eventId(Long eventId) {
        this.setEventId(eventId);
        return this;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public Integer getTotalQuantity() {
        return this.totalQuantity;
    }

    public TicketStock totalQuantity(Integer totalQuantity) {
        this.setTotalQuantity(totalQuantity);
        return this;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Integer getAvailableQuantity() {
        return this.availableQuantity;
    }

    public TicketStock availableQuantity(Integer availableQuantity) {
        this.setAvailableQuantity(availableQuantity);
        return this;
    }

    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TicketStock)) {
            return false;
        }
        return getId() != null && getId().equals(((TicketStock) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TicketStock{" +
            "id=" + getId() +
            ", ticketTypeId=" + getTicketTypeId() +
            ", eventId=" + getEventId() +
            ", totalQuantity=" + getTotalQuantity() +
            ", availableQuantity=" + getAvailableQuantity() +
            "}";
    }
}
