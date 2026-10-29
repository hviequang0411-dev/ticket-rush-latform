package com.ticketrush.catalog.service;

import com.ticketrush.catalog.service.dto.TicketTypeDTO;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link com.ticketrush.catalog.domain.TicketType}.
 */
public interface TicketTypeService {
    /**
     * Save a ticketType.
     *
     * @param ticketTypeDTO the entity to save.
     * @return the persisted entity.
     */
    TicketTypeDTO save(TicketTypeDTO ticketTypeDTO);

    /**
     * Updates a ticketType.
     *
     * @param ticketTypeDTO the entity to update.
     * @return the persisted entity.
     */
    TicketTypeDTO update(TicketTypeDTO ticketTypeDTO);

    /**
     * Partially updates a ticketType.
     *
     * @param ticketTypeDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<TicketTypeDTO> partialUpdate(TicketTypeDTO ticketTypeDTO);

    /**
     * Get all the ticketTypes.
     *
     * @return the list of entities.
     */
    List<TicketTypeDTO> findAll();

    /**
     * Get all the ticketTypes with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    List<TicketTypeDTO> findAllWithEagerRelationships();

    /**
     * Get the "id" ticketType.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<TicketTypeDTO> findOne(Long id);

    /**
     * Delete the "id" ticketType.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
