package com.ticketrush.payment.repository;

import com.ticketrush.payment.domain.PaymentWebhookEvent;
import com.ticketrush.payment.domain.enumeration.PaymentProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PaymentWebhookEvent entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
    Optional<PaymentWebhookEvent> findByProviderAndEventKey(PaymentProvider provider, String eventKey);
}
