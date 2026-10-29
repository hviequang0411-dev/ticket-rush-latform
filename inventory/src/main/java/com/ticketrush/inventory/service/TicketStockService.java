package com.ticketrush.inventory.service;

import com.ticketrush.inventory.service.dto.TicketStockDTO;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service Interface for managing {@link com.ticketrush.inventory.domain.TicketStock}.
 */
public interface TicketStockService {
    /**
     * Save a ticketStock.
     *
     * @param ticketStockDTO the entity to save.
     * @return the persisted entity.
     */
    TicketStockDTO save(TicketStockDTO ticketStockDTO);

    /**
     * Updates a ticketStock.
     *
     * @param ticketStockDTO the entity to update.
     * @return the persisted entity.
     */
    TicketStockDTO update(TicketStockDTO ticketStockDTO);

    /**
     * Partially updates a ticketStock.
     *
     * @param ticketStockDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<TicketStockDTO> partialUpdate(TicketStockDTO ticketStockDTO);

    /**
     * Get all the ticketStocks.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Page<TicketStockDTO> findAll(Pageable pageable);

    /**
     * Get the "id" ticketStock.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<TicketStockDTO> findOne(Long id);

    /**
     * Delete the "id" ticketStock.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
