package com.ticketrush.payment.service.flow;

import com.ticketrush.payment.domain.PaymentWebhookEvent;
import com.ticketrush.payment.domain.enumeration.PaymentProvider;
import com.ticketrush.payment.repository.PaymentWebhookEventRepository;
import java.time.Clock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Stores every provider callback once, in its own transaction, before anything is applied: the record survives
 * even if processing fails, and a redelivery (same provider and event key) finds the existing record.
 */
@Component
public class WebhookInbox {

    private final PaymentWebhookEventRepository repository;

    private final TransactionTemplate requiresNew;

    private final Clock clock;

    public WebhookInbox(PaymentWebhookEventRepository repository, TransactionTemplate transactionTemplate, Clock clock) {
        this.repository = repository;
        this.requiresNew = new TransactionTemplate(transactionTemplate.getTransactionManager());
        this.requiresNew.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
        this.clock = clock;
    }

    /**
     * @return the stored callback; {@code processedAt} is set if an earlier delivery was already applied.
     */
    @Transactional(propagation = Propagation.NEVER)
    public PaymentWebhookEvent record(PaymentProvider provider, String eventKey, String payload, boolean signatureValid) {
        try {
            return requiresNew.execute(status ->
                repository
                    .findByProviderAndEventKey(provider, eventKey)
                    .map(this::redelivered)
                    .orElseGet(() ->
                        repository.saveAndFlush(
                            new PaymentWebhookEvent()
                                .provider(provider)
                                .eventKey(eventKey)
                                .payload(payload)
                                .signatureValid(signatureValid)
                                .receivedAt(clock.instant())
                        )
                    )
            );
        } catch (DataIntegrityViolationException e) {
            // The same callback arrived concurrently and the other delivery stored it first
            return requiresNew.execute(status -> repository.findByProviderAndEventKey(provider, eventKey).map(this::redelivered).orElseThrow(() -> e));
        }
    }

    private PaymentWebhookEvent redelivered(PaymentWebhookEvent event) {
        repository.countRedelivery(event.getId(), clock.instant());
        return repository.findById(event.getId()).orElseThrow();
    }
}
