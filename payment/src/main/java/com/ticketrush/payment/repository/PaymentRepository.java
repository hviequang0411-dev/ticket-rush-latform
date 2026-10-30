package com.ticketrush.payment.repository;

import com.ticketrush.payment.domain.Payment;
import com.ticketrush.payment.domain.enumeration.PaymentStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Payment entity.
 */
@SuppressWarnings("unused")
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findFirstByOrderIdAndStatus(Long orderId, PaymentStatus status);

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    Optional<Payment> findByProviderRef(String providerRef);

    /** Serializes concurrent webhooks for one payment, so a result is applied exactly once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.providerRef = :providerRef")
    Optional<Payment> lockByProviderRef(@Param("providerRef") String providerRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.orderId = :orderId order by p.createdAt desc")
    List<Payment> lockByOrderId(@Param("orderId") Long orderId);

    @Query(
        value = "select * from payment where status = 'PENDING' and expires_at < :now order by expires_at limit :limit for update skip locked",
        nativeQuery = true
    )
    List<Payment> lockExpiredPending(@Param("now") Instant now, @Param("limit") int limit);

    /** Organiser search: {@code q} matches the order code; null status means any. */
    @Query(
        value = "select p from Payment p where lower(p.orderCode) like :q and (:status is null or p.status = :status) order by p.createdAt desc",
        countQuery = "select count(p) from Payment p where lower(p.orderCode) like :q and (:status is null or p.status = :status)"
    )
    Page<Payment> search(
        @Param("q") String likePattern,
        @Param("status") PaymentStatus status,
        Pageable pageable
    );
}
