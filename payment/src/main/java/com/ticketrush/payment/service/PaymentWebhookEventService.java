package com.ticketrush.payment.service;

import com.ticketrush.payment.service.dto.PaymentWebhookEventDTO;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for managing {@link com.ticketrush.payment.domain.PaymentWebhookEvent}.
 */
public interface PaymentWebhookEventService {
    /**
     * Save a paymentWebhookEvent.
     *
     * @param paymentWebhookEventDTO the entity to save.
     * @return the persisted entity.
     */
    PaymentWebhookEventDTO save(PaymentWebhookEventDTO paymentWebhookEventDTO);

    /**
     * Updates a paymentWebhookEvent.
     *
     * @param paymentWebhookEventDTO the entity to update.
     * @return the persisted entity.
     */
    PaymentWebhookEventDTO update(PaymentWebhookEventDTO paymentWebhookEventDTO);

    /**
     * Partially updates a paymentWebhookEvent.
     *
     * @param paymentWebhookEventDTO the entity to update partially.
     * @return the persisted entity.
     */
    Optional<PaymentWebhookEventDTO> partialUpdate(PaymentWebhookEventDTO paymentWebhookEventDTO);

    /**
     * Get all the paymentWebhookEvents.
     *
     * @return the list of entities.
     */
    List<PaymentWebhookEventDTO> findAll();

    /**
     * Get the "id" paymentWebhookEvent.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Optional<PaymentWebhookEventDTO> findOne(Long id);

    /**
     * Delete the "id" paymentWebhookEvent.
     *
     * @param id the id of the entity.
     */
    void delete(Long id);
}
