package com.ticketrush.catalog.service.criteria;

import com.ticketrush.catalog.domain.enumeration.EventStatus;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.*;

/**
 * Criteria class for the {@link com.ticketrush.catalog.domain.Event} entity. This class is used
 * in {@link com.ticketrush.catalog.web.rest.EventResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /events?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
public class EventCriteria implements Serializable, Criteria {

    /**
     * Class for filtering EventStatus
     */
    public static class EventStatusFilter extends Filter<EventStatus> {

        public EventStatusFilter() {}

        public EventStatusFilter(EventStatusFilter filter) {
            super(filter);
        }

        @Override
        public EventStatusFilter copy() {
            return new EventStatusFilter(this);
        }
    }

    @Serial
    private static final long serialVersionUID = 1L;

    private LongFilter id;

    private StringFilter name;

    private StringFilter slug;

    private InstantFilter startTime;

    private InstantFilter endTime;

    private EventStatusFilter status;

    private StringFilter posterUrl;

    private LongFilter ticketTypesId;

    private LongFilter venueId;

    private Boolean distinct;

    public EventCriteria() {}

    public EventCriteria(EventCriteria other) {
        this.id = other.optionalId().map(LongFilter::copy).orElse(null);
        this.name = other.optionalName().map(StringFilter::copy).orElse(null);
        this.slug = other.optionalSlug().map(StringFilter::copy).orElse(null);
        this.startTime = other.optionalStartTime().map(InstantFilter::copy).orElse(null);
        this.endTime = other.optionalEndTime().map(InstantFilter::copy).orElse(null);
        this.status = other.optionalStatus().map(EventStatusFilter::copy).orElse(null);
        this.posterUrl = other.optionalPosterUrl().map(StringFilter::copy).orElse(null);
        this.ticketTypesId = other.optionalTicketTypesId().map(LongFilter::copy).orElse(null);
        this.venueId = other.optionalVenueId().map(LongFilter::copy).orElse(null);
        this.distinct = other.distinct;
    }

    @Override
    public EventCriteria copy() {
        return new EventCriteria(this);
    }

    public LongFilter getId() {
        return id;
    }

    public Optional<LongFilter> optionalId() {
        return Optional.ofNullable(id);
    }

    public LongFilter id() {
        if (id == null) {
            setId(new LongFilter());
        }
        return id;
    }

    public void setId(LongFilter id) {
        this.id = id;
    }

    public StringFilter getName() {
        return name;
    }

    public Optional<StringFilter> optionalName() {
        return Optional.ofNullable(name);
    }

    public StringFilter name() {
        if (name == null) {
            setName(new StringFilter());
        }
        return name;
    }

    public void setName(StringFilter name) {
        this.name = name;
    }

    public StringFilter getSlug() {
        return slug;
    }

    public Optional<StringFilter> optionalSlug() {
        return Optional.ofNullable(slug);
    }

    public StringFilter slug() {
        if (slug == null) {
            setSlug(new StringFilter());
        }
        return slug;
    }

    public void setSlug(StringFilter slug) {
        this.slug = slug;
    }

    public InstantFilter getStartTime() {
        return startTime;
    }

    public Optional<InstantFilter> optionalStartTime() {
        return Optional.ofNullable(startTime);
    }

    public InstantFilter startTime() {
        if (startTime == null) {
            setStartTime(new InstantFilter());
        }
        return startTime;
    }

    public void setStartTime(InstantFilter startTime) {
        this.startTime = startTime;
    }

    public InstantFilter getEndTime() {
        return endTime;
    }

    public Optional<InstantFilter> optionalEndTime() {
        return Optional.ofNullable(endTime);
    }

    public InstantFilter endTime() {
        if (endTime == null) {
            setEndTime(new InstantFilter());
        }
        return endTime;
    }

    public void setEndTime(InstantFilter endTime) {
        this.endTime = endTime;
    }

    public EventStatusFilter getStatus() {
        return status;
    }

    public Optional<EventStatusFilter> optionalStatus() {
        return Optional.ofNullable(status);
    }

    public EventStatusFilter status() {
        if (status == null) {
            setStatus(new EventStatusFilter());
        }
        return status;
    }

    public void setStatus(EventStatusFilter status) {
        this.status = status;
    }

    public StringFilter getPosterUrl() {
        return posterUrl;
    }

    public Optional<StringFilter> optionalPosterUrl() {
        return Optional.ofNullable(posterUrl);
    }

    public StringFilter posterUrl() {
        if (posterUrl == null) {
            setPosterUrl(new StringFilter());
        }
        return posterUrl;
    }

    public void setPosterUrl(StringFilter posterUrl) {
        this.posterUrl = posterUrl;
    }

    public LongFilter getTicketTypesId() {
        return ticketTypesId;
    }

    public Optional<LongFilter> optionalTicketTypesId() {
        return Optional.ofNullable(ticketTypesId);
    }

    public LongFilter ticketTypesId() {
        if (ticketTypesId == null) {
            setTicketTypesId(new LongFilter());
        }
        return ticketTypesId;
    }

    public void setTicketTypesId(LongFilter ticketTypesId) {
        this.ticketTypesId = ticketTypesId;
    }

    public LongFilter getVenueId() {
        return venueId;
    }

    public Optional<LongFilter> optionalVenueId() {
        return Optional.ofNullable(venueId);
    }

    public LongFilter venueId() {
        if (venueId == null) {
            setVenueId(new LongFilter());
        }
        return venueId;
    }

    public void setVenueId(LongFilter venueId) {
        this.venueId = venueId;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public Optional<Boolean> optionalDistinct() {
        return Optional.ofNullable(distinct);
    }

    public Boolean distinct() {
        if (distinct == null) {
            setDistinct(true);
        }
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final EventCriteria that = (EventCriteria) o;
        return (
            Objects.equals(id, that.id) &&
            Objects.equals(name, that.name) &&
            Objects.equals(slug, that.slug) &&
            Objects.equals(startTime, that.startTime) &&
            Objects.equals(endTime, that.endTime) &&
            Objects.equals(status, that.status) &&
            Objects.equals(posterUrl, that.posterUrl) &&
            Objects.equals(ticketTypesId, that.ticketTypesId) &&
            Objects.equals(venueId, that.venueId) &&
            Objects.equals(distinct, that.distinct)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, slug, startTime, endTime, status, posterUrl, ticketTypesId, venueId, distinct);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "EventCriteria{" +
            optionalId().map(f -> "id=" + f + ", ").orElse("") +
            optionalName().map(f -> "name=" + f + ", ").orElse("") +
            optionalSlug().map(f -> "slug=" + f + ", ").orElse("") +
            optionalStartTime().map(f -> "startTime=" + f + ", ").orElse("") +
            optionalEndTime().map(f -> "endTime=" + f + ", ").orElse("") +
            optionalStatus().map(f -> "status=" + f + ", ").orElse("") +
            optionalPosterUrl().map(f -> "posterUrl=" + f + ", ").orElse("") +
            optionalTicketTypesId().map(f -> "ticketTypesId=" + f + ", ").orElse("") +
            optionalVenueId().map(f -> "venueId=" + f + ", ").orElse("") +
            optionalDistinct().map(f -> "distinct=" + f + ", ").orElse("") +
        "}";
    }
}
