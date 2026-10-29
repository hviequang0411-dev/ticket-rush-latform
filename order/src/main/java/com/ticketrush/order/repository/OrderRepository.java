package com.ticketrush.order.repository;

import com.ticketrush.order.domain.Order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
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
}
