package com.ticketrush.inventory.service;

import com.ticketrush.inventory.service.dto.StockHoldDTO;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link com.ticketrush.inventory.domain.StockHold}.
 */
public interface StockHoldService {
    /**
     * Save a stockHold.
     *
     * @param stockHoldDTO the entity to save.
     * @return the persisted entity.
     */
    StockHoldDTO save(StockHoldDTO stockHoldDTO);

    /**
     * Updates a stockHold.
     *
     * @param stockHoldDTO the entity to update.
     * @return the persisted entity.
     */
    StockHoldDTO update(StockHoldDTO stockHoldDTO);

    /**
     * Partially updates a stockHold.
     *
     * @param stockHoldDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<StockHoldDTO> partialUpdate(StockHoldDTO stockHoldDTO);

    /**
     * Get all the stockHolds.
     *
     * @return the list of entities.
     */
    List<StockHoldDTO> findAll();

    /**
     * Get the "id" stockHold.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<StockHoldDTO> findOne(Long id);

    /**
     * Delete the "id" stockHold.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
