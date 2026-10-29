package com.ticketrush.order.web.rest;

import com.ticketrush.order.repository.OrderRepository;
import com.ticketrush.order.service.OrderQueryService;
import com.ticketrush.order.service.OrderService;
import com.ticketrush.order.service.criteria.OrderCriteria;
import com.ticketrush.order.service.dto.OrderDTO;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.ticketrush.order.domain.Order}.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrderResource.class);

    private final OrderService orderService;

    private final OrderRepository orderRepository;

    private final OrderQueryService orderQueryService;

    public OrderResource(OrderService orderService, OrderRepository orderRepository, OrderQueryService orderQueryService) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.orderQueryService = orderQueryService;
    }

    /**
     * {@code GET  /orders} : get all the Orders.
     *
     * @param pageable the pagination information.
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Orders in body.
     */
    @GetMapping("")
    public ResponseEntity<List<OrderDTO>> getAllOrders(
        OrderCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        LOG.debug("REST request to get Orders by criteria: {}", criteria);

        Page<OrderDTO> page = orderQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /orders/count} : count all the orders.
     *
     * @param criteria the criteria which the requested entities should match.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the count in body.
     */
    @GetMapping("/count")
    public ResponseEntity<Long> countOrders(OrderCriteria criteria) {
        LOG.debug("REST request to count Orders by criteria: {}", criteria);
        return ResponseEntity.ok().body(orderQueryService.countByCriteria(criteria));
    }

    /**
     * {@code GET  /orders/:id} : get the "id" order.
     *
     * @param id the id of the orderDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the orderDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrder(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Order : {}", id);
        Optional<OrderDTO> orderDTO = orderService.findOne(id);
        return ResponseUtil.wrapOrNotFound(orderDTO);
    }
}
