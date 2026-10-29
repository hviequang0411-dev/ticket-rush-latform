package com.ticketrush.inventory.repository;

import com.ticketrush.inventory.domain.StockHold;
import com.ticketrush.inventory.domain.enumeration.HoldStatus;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the StockHold entity.
 */
@SuppressWarnings("unused")
@Repository
public interface StockHoldRepository extends JpaRepository<StockHold, Long> {
    @EntityGraph(attributePaths = "stock")
    List<StockHold> findAllByOrderId(Long orderId);

    @EntityGraph(attributePaths = "stock")
    List<StockHold> findAllByOrderIdAndStatus(Long orderId, HoldStatus status);

    /**
     * Locks a batch of expired holds. {@code SKIP LOCKED} lets several instances run the expiry job at once
     * without blocking each other or expiring the same hold twice.
     */
    @Query(
        value = "select * from stock_hold where status = 'HELD' and expires_at < :now " +
        "order by expires_at limit :limit for update skip locked",
        nativeQuery = true
    )
    List<StockHold> lockExpiredHolds(@Param("now") Instant now, @Param("limit") int limit);
}
