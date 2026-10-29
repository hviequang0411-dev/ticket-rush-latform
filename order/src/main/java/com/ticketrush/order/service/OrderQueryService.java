package com.ticketrush.order.service;

import com.ticketrush.order.domain.*; // for static metamodels
import com.ticketrush.order.domain.Order;
import com.ticketrush.order.repository.OrderRepository;
import com.ticketrush.order.service.criteria.OrderCriteria;
import com.ticketrush.order.service.dto.OrderDTO;
import com.ticketrush.order.service.mapper.OrderMapper;
import jakarta.persistence.criteria.JoinType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.jhipster.service.QueryService;

/**
 * Service for executing complex queries for {@link Order} entities in the database.
 * The main input is a {@link OrderCriteria} which gets converted to {@link Specification},
 * in a way that all the filters must apply.
 * It returns a {@link Page} of {@link OrderDTO} which fulfills the criteria.
 */
@Service
@Transactional(readOnly = true)
public class OrderQueryService extends QueryService<Order> {

    private static final Logger LOG = LoggerFactory.getLogger(OrderQueryService.class);

    private final OrderRepository orderRepository;

    private final OrderMapper orderMapper;

    public OrderQueryService(OrderRepository orderRepository, OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    /**
     * Return a {@link Page} of {@link OrderDTO} which matches the criteria from the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @param page The page, which should be returned.
     * @return the matching entities.
     */
    @Transactional(readOnly = true)
    public Page<OrderDTO> findByCriteria(OrderCriteria criteria, Pageable page) {
        LOG.debug("find by criteria : {}, page: {}", criteria, page);
        final Specification<Order> specification = createSpecification(criteria);
        return orderRepository.findAll(specification, page).map(orderMapper::toDto);
    }

    /**
     * Return the number of matching entities in the database.
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the number of matching entities.
     */
    @Transactional(readOnly = true)
    public long countByCriteria(OrderCriteria criteria) {
        LOG.debug("count by criteria : {}", criteria);
        final Specification<Order> specification = createSpecification(criteria);
        return orderRepository.count(specification);
    }

    /**
     * Function to convert {@link OrderCriteria} to a {@link Specification}
     * @param criteria The object which holds all the filters, which the entities should match.
     * @return the matching {@link Specification} of the entity.
     */
    protected Specification<Order> createSpecification(OrderCriteria criteria) {
        Specification<Order> specification = Specification.unrestricted();
        if (criteria != null) {
            // This has to be called first, because the distinct method returns null
            specification = specification.and(
                Specification.allOf(
                    Boolean.TRUE.equals(criteria.getDistinct()) ? distinct(criteria.getDistinct()) : Specification.unrestricted(),
                    buildRangeSpecification(criteria.getId(), Order_.id),
                    buildStringSpecification(criteria.getCode(), Order_.code),
                    buildStringSpecification(criteria.getUserLogin(), Order_.userLogin),
                    buildSpecification(criteria.getStatus(), Order_.status),
                    buildRangeSpecification(criteria.getTotalAmount(), Order_.totalAmount),
                    buildStringSpecification(criteria.getCurrency(), Order_.currency),
                    buildStringSpecification(criteria.getIdempotencyKey(), Order_.idempotencyKey),
                    buildStringSpecification(criteria.getFailureReason(), Order_.failureReason),
                    buildRangeSpecification(criteria.getExpiresAt(), Order_.expiresAt),
                    buildRangeSpecification(criteria.getCreatedAt(), Order_.createdAt),
                    buildRangeSpecification(criteria.getUpdatedAt(), Order_.updatedAt),
                    buildSpecification(criteria.getItemsId(), root -> root.join(Order_.itemses, JoinType.LEFT).get(OrderItem_.id))
                )
            );
        }
        return specification;
    }
}
