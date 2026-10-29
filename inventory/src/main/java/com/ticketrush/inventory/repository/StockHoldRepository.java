package com.ticketrush.inventory.repository;

import com.ticketrush.inventory.domain.StockHold;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the StockHold entity.
 */
@SuppressWarnings("unused")
@Repository
public interface StockHoldRepository extends JpaRepository<StockHold, Long> {}
