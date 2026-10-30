package com.ticketrush.order.web.rest;

import com.ticketrush.order.domain.enumeration.OrderStatus;
import com.ticketrush.order.service.admin.AdminOrder;
import com.ticketrush.order.service.admin.AdminOrderService;
import com.ticketrush.order.service.admin.SalesStats;
import com.ticketrush.order.service.checkout.OrderNotFoundException;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * Orders of every buyer and the sales dashboard, for organisers ({@code /api/admin/**} is admins only).
 */
@RestController
@RequestMapping("/api/admin")
@Validated
public class AdminOrderResource {

    private final AdminOrderService adminOrderService;

    public AdminOrderResource(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    /**
     * {@code GET /api/admin/orders?q=&status=} : newest first; {@code q} matches the order code or the buyer.
     */
    @GetMapping("/orders")
    public ResponseEntity<List<AdminOrder>> search(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) OrderStatus status,
        @ParameterObject Pageable pageable
    ) {
        Page<AdminOrder> page = adminOrderService.search(q, status, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET /api/admin/orders/{code}} : one order with its tickets.
     */
    @GetMapping("/orders/{code}")
    public AdminOrder findOne(@PathVariable String code) {
        return adminOrderService.findOne(code).orElseThrow(() -> new OrderNotFoundException(code));
    }

    /**
     * {@code GET /api/admin/stats/sales?days=14&zone=Asia/Ho_Chi_Minh&paceMinutes=10}
     */
    @GetMapping("/stats/sales")
    public SalesStats sales(
        @RequestParam(defaultValue = "14") @Min(1) @Max(90) int days,
        @RequestParam(defaultValue = "Asia/Ho_Chi_Minh") String zone,
        @RequestParam(defaultValue = "10") @Min(1) @Max(1440) int paceMinutes
    ) {
        ZoneId zoneId;
        try {
            zoneId = ZoneId.of(zone);
        } catch (DateTimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown time zone " + zone);
        }
        return adminOrderService.stats(zoneId, days, paceMinutes);
    }
}
