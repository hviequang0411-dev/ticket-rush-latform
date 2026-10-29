package com.ticketrush.inventory.repository;

import com.ticketrush.inventory.domain.TicketStock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the TicketStock entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TicketStockRepository extends JpaRepository<TicketStock, Long> {
    Optional<TicketStock> findByTicketTypeId(Long ticketTypeId);

    List<TicketStock> findAllByTicketTypeIdIn(Collection<Long> ticketTypeIds);

    /**
     * Takes {@code quantity} seats only if that many are still available. The check and the write are one
     * statement, so concurrent callers serialize on the row lock and can never drive the stock below zero.
     *
     * <p>
     * Deliberately no {@code clearAutomatically}: callers keep working with managed StockHold entities in the
     * same transaction, and clearing would silently drop their pending changes. Managed TicketStock instances
     * keep a stale availableQuantity afterwards, so don't read it after calling this.
     *
     * @return 1 if the seats were taken, 0 if there were not enough left.
     */
    @Modifying(flushAutomatically = true)
    @Query(
        "update TicketStock s set s.availableQuantity = s.availableQuantity - :quantity, s.version = s.version + 1 " +
        "where s.id = :id and s.availableQuantity >= :quantity"
    )
    int tryTake(@Param("id") Long id, @Param("quantity") int quantity);

    /**
     * Gives seats back, never above the configured total.
     *
     * @return 1 if the seats were returned, 0 if that would exceed the total (a bug elsewhere; logged by the caller).
     */
    @Modifying(flushAutomatically = true)
    @Query(
        "update TicketStock s set s.availableQuantity = s.availableQuantity + :quantity, s.version = s.version + 1 " +
        "where s.id = :id and s.availableQuantity + :quantity <= s.totalQuantity"
    )
    int giveBack(@Param("id") Long id, @Param("quantity") int quantity);
}
