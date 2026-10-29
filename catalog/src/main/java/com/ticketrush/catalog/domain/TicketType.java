package com.ticketrush.catalog.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A TicketType.
 */
@Entity
@Table(name = "ticket_type")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TicketType implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @NotNull
    @DecimalMin(value = "0")
    @Column(name = "price", precision = 21, scale = 2, nullable = false)
    private BigDecimal price;

    @NotNull
    @Size(min = 3, max = 3)
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @NotNull
    @Min(value = 1)
    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "sale_start")
    private Instant saleStart;

    @Column(name = "sale_end")
    private Instant saleEnd;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "ticketTypeses", "venue" }, allowSetters = true)
    private Event event;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public TicketType id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public TicketType name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public TicketType price(BigDecimal price) {
        this.setPrice(price);
        return this;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getCurrency() {
        return this.currency;
    }

    public TicketType currency(String currency) {
        this.setCurrency(currency);
        return this;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getTotalQuantity() {
        return this.totalQuantity;
    }

    public TicketType totalQuantity(Integer totalQuantity) {
        this.setTotalQuantity(totalQuantity);
        return this;
    }

    public void setTotalQuantity(Integer totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Instant getSaleStart() {
        return this.saleStart;
    }

    public TicketType saleStart(Instant saleStart) {
        this.setSaleStart(saleStart);
        return this;
    }

    public void setSaleStart(Instant saleStart) {
        this.saleStart = saleStart;
    }

    public Instant getSaleEnd() {
        return this.saleEnd;
    }

    public TicketType saleEnd(Instant saleEnd) {
        this.setSaleEnd(saleEnd);
        return this;
    }

    public void setSaleEnd(Instant saleEnd) {
        this.saleEnd = saleEnd;
    }

    public Event getEvent() {
        return this.event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public TicketType event(Event event) {
        this.setEvent(event);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TicketType)) {
            return false;
        }
        return getId() != null && getId().equals(((TicketType) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "TicketType{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", price=" + getPrice() +
            ", currency='" + getCurrency() + "'" +
            ", totalQuantity=" + getTotalQuantity() +
            ", saleStart='" + getSaleStart() + "'" +
            ", saleEnd='" + getSaleEnd() + "'" +
            "}";
    }
}
