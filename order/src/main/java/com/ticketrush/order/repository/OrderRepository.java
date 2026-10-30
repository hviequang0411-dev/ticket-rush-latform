package com.ticketrush.order.repository;

import com.ticketrush.order.domain.Order;
import com.ticketrush.order.domain.enumeration.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Order entity.
 */
@SuppressWarnings("unused")
@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    @EntityGraph(attributePaths = "itemses")
    Optional<Order> findOneWithItemsByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = "itemses")
    Optional<Order> findOneWithItemsByCodeAndUserLogin(String code, String userLogin);

    @EntityGraph(attributePaths = "itemses")
    Optional<Order> findOneWithItemsById(Long id);

    Page<Order> findAllByUserLoginOrderByCreatedAtDesc(String userLogin, Pageable pageable);

    @EntityGraph(attributePaths = "itemses")
    Optional<Order> findOneWithItemsByCode(String code);

    /** Organiser search: {@code q} matches the order code or the buyer's login; null status means any. */
    @Query(
        "select o from Order o where (lower(o.code) like :q or lower(o.userLogin) like :q) " +
        "and (:status is null or o.status = :status) order by o.createdAt desc"
    )
    Page<Order> search(@Param("q") String likePattern, @Param("status") OrderStatus status, Pageable pageable);

    long countByStatusIn(Collection<OrderStatus> statuses);

    /** Orders placed since {@code from}, per status. */
    @Query("select o.status as status, count(o) as orders from Order o where o.createdAt >= :from and o.createdAt < :to group by o.status")
    List<OrdersByStatus> countByStatusPlacedBetween(@Param("from") Instant from, @Param("to") Instant to);

    /** Confirmed revenue per local day; an order counts on the day it was confirmed. */
    @Query(
        value = "select cast(updated_at at time zone :zone as date) as day, sum(total_amount) as revenue, count(*) as orders " +
        "from ticket_order where status = 'CONFIRMED' and updated_at >= :from group by 1 order by 1",
        nativeQuery = true
    )
    List<DailyRevenue> dailyRevenue(@Param("from") Instant from, @Param("zone") String zone);

    /** Seats confirmed per event since {@code from}: the sale's pace. */
    @Query(
        "select i.eventId as eventId, sum(i.quantity) as seats from OrderItem i join i.order o " +
        "where o.status = com.ticketrush.order.domain.enumeration.OrderStatus.CONFIRMED and o.updatedAt >= :from group by i.eventId"
    )
    List<SeatsByEvent> seatsConfirmedSince(@Param("from") Instant from);

    interface OrdersByStatus {
        OrderStatus getStatus();

        Long getOrders();
    }

    interface DailyRevenue {
        LocalDate getDay();

        BigDecimal getRevenue();

        Long getOrders();
    }

    interface SeatsByEvent {
        Long getEventId();

        Long getSeats();
    }
}
