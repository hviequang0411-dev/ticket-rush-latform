package com.ticketrush.payment.repository;

import com.ticketrush.payment.domain.OutboxEvent;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    /**
     * Oldest unpublished events, locked for this relay. {@code SKIP LOCKED} lets several instances relay at once
     * without sending the same event twice.
     */
    @Query(
        value = "select * from outbox_event where published_at is null order by created_at limit :limit for update skip locked",
        nativeQuery = true
    )
    List<OutboxEvent> lockUnpublished(@Param("limit") int limit);

    long countByPublishedAtIsNull();

    Optional<OutboxEvent> findFirstByPublishedAtIsNullOrderByCreatedAtAsc();

    @Modifying
    @Query("delete from OutboxEvent e where e.publishedAt < :before")
    int deletePublishedBefore(@Param("before") Instant before);
}
