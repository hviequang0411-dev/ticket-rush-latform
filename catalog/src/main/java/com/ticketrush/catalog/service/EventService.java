package com.ticketrush.catalog.service;

import com.ticketrush.catalog.service.dto.EventDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.ticketrush.catalog.domain.Event}.
 */
public interface EventService {
    /**
     * Save a event.
     *
     * @param eventDTO the entity to save.
     * @return the persisted entity.
     */
    EventDTO save(EventDTO eventDTO);

    /**
     * Updates a event.
     *
     * @param eventDTO the entity to update.
     * @return the persisted entity.
     */
    EventDTO update(EventDTO eventDTO);

    /**
     * Partially updates a event.
     *
     * @param eventDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<EventDTO> partialUpdate(EventDTO eventDTO);

    /**
     * Get all the events with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<EventDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Get the "id" event.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<EventDTO> findOne(Long id);

    /**
     * Delete the "id" event.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
