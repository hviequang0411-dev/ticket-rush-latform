package com.ticketrush.order.web.rest;

import com.ticketrush.order.repository.OrderItemRepository;
import com.ticketrush.order.service.OrderItemService;
import com.ticketrush.order.service.dto.OrderItemDTO;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.ticketrush.order.domain.OrderItem}.
 */
@RestController
@RequestMapping("/api/order-items")
public class OrderItemResource {

    private static final Logger LOG = LoggerFactory.getLogger(OrderItemResource.class);

    private final OrderItemService orderItemService;

    private final OrderItemRepository orderItemRepository;

    public OrderItemResource(OrderItemService orderItemService, OrderItemRepository orderItemRepository) {
        this.orderItemService = orderItemService;
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * {@code GET  /order-items} : get all the Order Items.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Order Items in body.
     */
    @GetMapping("")
    public List<OrderItemDTO> getAllOrderItems() {
        LOG.debug("REST request to get all OrderItems");
        return orderItemService.findAll();
    }

    /**
     * {@code GET  /order-items/:id} : get the "id" orderItem.
     *
     * @param id the id of the orderItemDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the orderItemDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderItemDTO> getOrderItem(@PathVariable("id") Long id) {
        LOG.debug("REST request to get OrderItem : {}", id);
        Optional<OrderItemDTO> orderItemDTO = orderItemService.findOne(id);
        return ResponseUtil.wrapOrNotFound(orderItemDTO);
    }
}
