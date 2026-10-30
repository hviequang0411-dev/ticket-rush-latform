package com.ticketrush.catalog.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.ticketrush.catalog.domain.enumeration.EventStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Event.
 */
@Entity
@Table(name = "event")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Event implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(max = 200)
    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @NotNull
    @Size(max = 220)
    @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$")
    @Column(name = "slug", length = 220, nullable = false, unique = true)
    private String slug;

    @Lob
    @Column(name = "description")
    private String description;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EventStatus status;

    @Size(max = 1000)
    @Column(name = "poster_url", length = 1000)
    private String posterUrl;

    /** Buyers must queue in the waiting room and hold an admission pass to check out. */
    @Column(name = "waiting_room", nullable = false)
    private Boolean waitingRoom = false;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "event")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "event" }, allowSetters = true)
    private Set<TicketType> ticketTypeses = new HashSet<>();

    @ManyToOne(optional = false)
    @NotNull
    private Venue venue;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Event id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public Event name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return this.slug;
    }

    public Event slug(String slug) {
        this.setSlug(slug);
        return this;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getDescription() {
        return this.description;
    }

    public Event description(String description) {
        this.setDescription(description);
        return this;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getStartTime() {
        return this.startTime;
    }

    public Event startTime(Instant startTime) {
        this.setStartTime(startTime);
        return this;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return this.endTime;
    }

    public Event endTime(Instant endTime) {
        this.setEndTime(endTime);
        return this;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public EventStatus getStatus() {
        return this.status;
    }

    public Event status(EventStatus status) {
        this.setStatus(status);
        return this;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public Boolean getWaitingRoom() {
        return this.waitingRoom;
    }

    public Event waitingRoom(Boolean waitingRoom) {
        this.setWaitingRoom(waitingRoom);
        return this;
    }

    public void setWaitingRoom(Boolean waitingRoom) {
        this.waitingRoom = waitingRoom != null && waitingRoom;
    }

    public String getPosterUrl() {
        return this.posterUrl;
    }

    public Event posterUrl(String posterUrl) {
        this.setPosterUrl(posterUrl);
        return this;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public Set<TicketType> getTicketTypeses() {
        return this.ticketTypeses;
    }

    public void setTicketTypeses(Set<TicketType> ticketTypes) {
        if (this.ticketTypeses != null) {
            this.ticketTypeses.forEach(i -> i.setEvent(null));
        }
        if (ticketTypes != null) {
            ticketTypes.forEach(i -> i.setEvent(this));
        }
        this.ticketTypeses = ticketTypes;
    }

    public Event ticketTypeses(Set<TicketType> ticketTypes) {
        this.setTicketTypeses(ticketTypes);
        return this;
    }

    public Event addTicketTypes(TicketType ticketType) {
        this.ticketTypeses.add(ticketType);
        ticketType.setEvent(this);
        return this;
    }

    public Event removeTicketTypes(TicketType ticketType) {
        this.ticketTypeses.remove(ticketType);
        ticketType.setEvent(null);
        return this;
    }

    public Venue getVenue() {
        return this.venue;
    }

    public void setVenue(Venue venue) {
        this.venue = venue;
    }

    public Event venue(Venue venue) {
        this.setVenue(venue);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Event)) {
            return false;
        }
        return getId() != null && getId().equals(((Event) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Event{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", slug='" + getSlug() + "'" +
            ", description='" + getDescription() + "'" +
            ", startTime='" + getStartTime() + "'" +
            ", endTime='" + getEndTime() + "'" +
            ", status='" + getStatus() + "'" +
            ", posterUrl='" + getPosterUrl() + "'" +
            "}";
    }
}
