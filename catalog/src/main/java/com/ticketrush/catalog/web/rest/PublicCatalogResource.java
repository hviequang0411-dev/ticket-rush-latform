package com.ticketrush.catalog.web.rest;

import com.ticketrush.catalog.service.publiccatalog.PublicCatalogService;
import com.ticketrush.catalog.service.publiccatalog.PublicEvent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * Events for anonymous visitors (permitAll, read-only). Signed-in admins keep using /api/events.
 */
@RestController
@RequestMapping("/api/public/events")
@Validated
public class PublicCatalogResource {

    private final PublicCatalogService publicCatalogService;

    public PublicCatalogResource(PublicCatalogService publicCatalogService) {
        this.publicCatalogService = publicCatalogService;
    }

    /**
     * {@code GET /api/public/events} : events on sale, soonest first, with venue and ticket types.
     */
    @GetMapping("")
    public List<PublicEvent> onSale(@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return publicCatalogService.listOnSale(page, size);
    }

    /**
     * {@code GET /api/public/events/{slug}} : one event on sale.
     */
    @GetMapping("/{slug}")
    public ResponseEntity<PublicEvent> bySlug(@PathVariable String slug) {
        return ResponseUtil.wrapOrNotFound(publicCatalogService.findBySlug(slug));
    }

    /**
     * {@code GET /api/public/events/by-id/{id}} : an event a past order refers to, also when it ended or was cancelled.
     */
    @GetMapping("/by-id/{id}")
    public ResponseEntity<PublicEvent> byId(@PathVariable Long id) {
        return ResponseUtil.wrapOrNotFound(publicCatalogService.findById(id));
    }
}
