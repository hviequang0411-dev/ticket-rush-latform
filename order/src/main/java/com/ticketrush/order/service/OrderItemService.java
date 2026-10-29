package com.ticketrush.order.service;

import com.ticketrush.order.service.dto.OrderItemDTO;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link com.ticketrush.order.domain.OrderItem}.
 */
public interface OrderItemService {
    /**
     * Save a orderItem.
     *
     * @param orderItemDTO the entity to save.
     * @return the persisted entity.
     */
    OrderItemDTO save(OrderItemDTO orderItemDTO);

    /**
     * Updates a orderItem.
     *
     * @param orderItemDTO the entity to update.
     * @return the persisted entity.
     */
    OrderItemDTO update(OrderItemDTO orderItemDTO);

    /**
     * Partially updates a orderItem.
     *
     * @param orderItemDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<OrderItemDTO> partialUpdate(OrderItemDTO orderItemDTO);

    /**
     * Get all the orderItems.
     *
     * @return the list of entities.
     */
    List<OrderItemDTO> findAll();

    /**
     * Get the "id" orderItem.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<OrderItemDTO> findOne(Long id);

    /**
     * Delete the "id" orderItem.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
