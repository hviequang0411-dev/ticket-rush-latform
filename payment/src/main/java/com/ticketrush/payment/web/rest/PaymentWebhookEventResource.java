package com.ticketrush.payment.web.rest;

import com.ticketrush.payment.service.PaymentWebhookEventService;
import com.ticketrush.payment.service.dto.PaymentWebhookEventDTO;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.ticketrush.payment.domain.PaymentWebhookEvent}.
 */
@RestController
@RequestMapping("/api/payment-webhook-events")
public class PaymentWebhookEventResource {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentWebhookEventResource.class);

    private final PaymentWebhookEventService paymentWebhookEventService;

    public PaymentWebhookEventResource(PaymentWebhookEventService paymentWebhookEventService) {
        this.paymentWebhookEventService = paymentWebhookEventService;
    }

    /**
     * {@code GET  /payment-webhook-events} : get all the Payment Webhook Events.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Payment Webhook Events in body.
     */
    @GetMapping("")
    public List<PaymentWebhookEventDTO> getAllPaymentWebhookEvents() {
        LOG.debug("REST request to get all PaymentWebhookEvents");
        return paymentWebhookEventService.findAll();
    }

    /**
     * {@code GET  /payment-webhook-events/:id} : get the "id" paymentWebhookEvent.
     *
     * @param id the id of the paymentWebhookEventDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the paymentWebhookEventDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentWebhookEventDTO> getPaymentWebhookEvent(@PathVariable("id") Long id) {
        LOG.debug("REST request to get PaymentWebhookEvent : {}", id);
        Optional<PaymentWebhookEventDTO> paymentWebhookEventDTO = paymentWebhookEventService.findOne(id);
        return ResponseUtil.wrapOrNotFound(paymentWebhookEventDTO);
    }
}
