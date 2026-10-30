package com.ticketrush.order.web.rest;

import com.ticketrush.order.security.SecurityUtils;
import com.ticketrush.order.service.checkout.CheckoutRequest;
import com.ticketrush.order.service.checkout.CheckoutResult;
import com.ticketrush.order.service.checkout.CheckoutService;
import com.ticketrush.order.service.checkout.OrderNotFoundException;
import com.ticketrush.order.service.checkout.OrderSummary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * Buying tickets and following your own orders.
 */
@RestController
@RequestMapping("/api")
@Validated
public class CheckoutResource {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    /** Admission pass from the gateway's waiting room, for events that sell through it. */
    public static final String QUEUE_PASS = "X-Queue-Pass";

    private static final Logger LOG = LoggerFactory.getLogger(CheckoutResource.class);

    private final CheckoutService checkoutService;

    public CheckoutResource(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    /**
     * {@code POST /api/checkout} : place an order and hold its seats for 10 minutes.
     * <p>
     * Clients send a fresh {@code Idempotency-Key} (for example a UUID) per purchase attempt and reuse it when
     * retrying: the retry returns the original order with status 200 instead of buying twice.
     *
     * @return 201 with the RESERVED order, 200 for a replay, 409 when sold out, 428 when the event needs a waiting
     *     room pass, 503 when a dependency is down.
     */
    @PostMapping("/checkout")
    public ResponseEntity<OrderSummary> checkout(
        @RequestHeader(IDEMPOTENCY_KEY) @Pattern(regexp = "[A-Za-z0-9_-]{8,64}") String idempotencyKey,
        @RequestHeader(value = QUEUE_PASS, required = false) String queuePass,
        @Valid @RequestBody CheckoutRequest request
    ) {
        LOG.debug("REST request to checkout : {}", request);
        CheckoutResult result = checkoutService.checkout(currentUser(), idempotencyKey, request, queuePass);
        if (result.replayed()) {
            return ResponseEntity.ok(result.order());
        }
        URI location = URI.create("/api/my-orders/" + result.order().code());
        return ResponseEntity.created(location).body(result.order());
    }

    /**
     * {@code GET /api/my-orders} : the current user's orders, newest first.
     */
    @GetMapping("/my-orders")
    public ResponseEntity<List<OrderSummary>> myOrders(@ParameterObject Pageable pageable) {
        Page<OrderSummary> page = checkoutService.findMine(currentUser(), pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET /api/my-orders/{code}} : one of the current user's orders.
     */
    @GetMapping("/my-orders/{code}")
    public OrderSummary myOrder(@PathVariable String code) {
        return checkoutService.findMine(currentUser(), code).orElseThrow(() -> new OrderNotFoundException(code));
    }

    /**
     * {@code POST /api/my-orders/{code}/cancel} : cancel an unpaid order and release its seats.
     */
    @PostMapping("/my-orders/{code}/cancel")
    public OrderSummary cancel(@PathVariable String code) {
        LOG.debug("REST request to cancel order : {}", code);
        return checkoutService.cancel(currentUser(), code);
    }

    private static String currentUser() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new AccessDeniedException("Not signed in"));
    }
}
