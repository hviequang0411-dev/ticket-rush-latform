package com.ticketrush.catalog.service;

import com.ticketrush.catalog.domain.*; // for static metamodels
import com.ticketrush.catalog.domain.Event;
import com.ticketrush.catalog.repository.EventRepository;
import com.ticketrush.catalog.service.criteria.EventCriteria;
import com.ticketrush.catalog.service.dto.EventDTO;
import com.ticketrush.catalog.service.mapper.EventMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Event} entities in the database.
 * The main input is a {@link EventCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link EventDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class EventQueryService extends QueryService<Event> {

    private static final Logger LOG = LoggerFactory.getLogger(EventQueryService.class);

    private final EventRepository eventRepository;

    private final EventMapper eventMapper;

    public EventQueryService(EventRepository eventRepository, EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
    }

    /**
     * Return a {@link Page} of {@link EventDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<EventDTO> findByCriteria(EventCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Event> specification = createSpecification(criteria);
        return eventRepository.findAll(specification, page).map(eventMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(EventCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Event> specification = createSpecification(criteria);
        return eventRepository.count(specification);
    }

    /**
     * Function to convert {@link EventCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Event> createSpecification(EventCriteria criteria) {
        Specification<Event> specification = Specification.unrestricted();
        specification = specification.and((root, query, builder) -> {
            if (Long.class != query.getResultType()) {
                root.fetch(Event_.venue, JoinType.LEFT);
            }
            return null;
        });
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), Event_.id),
                    buildStringSpecification(criteria.getName(), Event_.name),
                    buildStringSpecification(criteria.getSlug(), Event_.slug),
                    buildRangeSpecification(criteria.getStartTime(), Event_.startTime),
                    buildRangeSpecification(criteria.getEndTime(), Event_.endTime),
                    buildSpecification(criteria.getStatus(), Event_.status),
                    buildStringSpecification(criteria.getPosterUrl(), Event_.posterUrl),
                    buildSpecification(criteria.getTicketTypesId(), root ->
                        root.join(Event_.ticketTypeses, JoinType.LEFT).get(TicketType_.id)
                    ),
                    buildSpecification(criteria.getVenueId(), root -> root.join(Event_.venue, JoinType.LEFT).get(Venue_.id))
                )
            );
        }
        return specification;
    }
}
