package com.ticketrush.inventory.repository;

import com.ticketrush.inventory.domain.TicketStock;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the TicketStock entity.
 */
@SuppressWarnings("unused")
@Repository
public interface TicketStockRepository extends JpaRepository<TicketStock, Long> {}
