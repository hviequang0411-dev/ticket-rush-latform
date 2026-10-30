package com.ticketrush.order.repository;

import com.ticketrush.order.domain.Ticket;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findOneByCode(String code);

    List<Ticket> findAllByUserLoginOrderByIssuedAtDescIdAsc(String userLogin);

    List<Ticket> findAllByOrderIdOrderByIdAsc(Long orderId);

    boolean existsByOrderId(Long orderId);

    /**
     * Admits the holder: VALID -> USED in one conditional statement, so two gates scanning the same ticket at the
     * same moment cannot both let it in (the row lock makes the second see USED and match nothing).
     *
     * @return 1 if this call admitted the ticket, 0 if it was not VALID.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        "update Ticket t set t.status = com.ticketrush.order.domain.enumeration.TicketStatus.USED, t.checkedInAt = :at, t.checkedInBy = :by " +
        "where t.code = :code and t.status = com.ticketrush.order.domain.enumeration.TicketStatus.VALID"
    )
    int checkIn(@Param("code") String code, @Param("at") Instant at, @Param("by") String by);
}
