package com.ticketrush.inventory.repository;

import com.ticketrush.inventory.domain.StockHold;
import com.ticketrush.inventory.domain.enumeration.HoldStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
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

    /** Seats per ticket type and hold status, for the organiser's stock view. */
    @Query(
        "select h.stock.ticketTypeId as ticketTypeId, h.status as status, sum(h.quantity) as quantity from StockHold h " +
        "where h.stock.ticketTypeId in :ticketTypeIds and h.status in :statuses group by h.stock.ticketTypeId, h.status"
    )
    List<SeatsByStatus> sumSeats(@Param("ticketTypeIds") Collection<Long> ticketTypeIds, @Param("statuses") Collection<HoldStatus> statuses);

    @EntityGraph(attributePaths = "stock")
    List<StockHold> findAllByStatusOrderByExpiresAtAsc(HoldStatus status, Pageable pageable);

    interface SeatsByStatus {
        Long getTicketTypeId();

        HoldStatus getStatus();

        Long getQuantity();
    }
}
