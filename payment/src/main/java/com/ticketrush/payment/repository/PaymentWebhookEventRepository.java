package com.ticketrush.payment.repository;

import com.ticketrush.payment.domain.PaymentWebhookEvent;
import com.ticketrush.payment.domain.enumeration.PaymentProvider;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the PaymentWebhookEvent entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEvent, Long> {
    Optional<PaymentWebhookEvent> findByProviderAndEventKey(PaymentProvider provider, String eventKey);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PaymentWebhookEvent e set e.deliveries = e.deliveries + 1, e.lastReceivedAt = :at where e.id = :id")
    int countRedelivery(@Param("id") Long id, @Param("at") Instant at);

    @EntityGraph(attributePaths = "payment")
    @Query(value = "select e from PaymentWebhookEvent e order by e.receivedAt desc", countQuery = "select count(e) from PaymentWebhookEvent e")
    Page<PaymentWebhookEvent> findAllNewestFirst(Pageable pageable);
}
