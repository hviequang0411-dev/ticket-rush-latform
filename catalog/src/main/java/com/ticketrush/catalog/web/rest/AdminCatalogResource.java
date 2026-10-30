package com.ticketrush.catalog.web.rest;

import com.ticketrush.catalog.domain.enumeration.EventStatus;
import com.ticketrush.catalog.service.admin.AdminCatalogService;
import com.ticketrush.catalog.service.admin.EventForm;
import com.ticketrush.catalog.service.publiccatalog.PublicEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * Event management for organisers ({@code /api/admin/**} is admins only).
 */
@RestController
@RequestMapping("/api/admin/events")
public class AdminCatalogResource {

    private final AdminCatalogService adminCatalogService;

    public AdminCatalogResource(AdminCatalogService adminCatalogService) {
        this.adminCatalogService = adminCatalogService;
    }

    /**
     * {@code GET /api/admin/events?q=} : every event, any status, newest start first; {@code q} matches name, slug or city.
     */
    @GetMapping("")
    public ResponseEntity<List<PublicEvent>> search(@RequestParam(required = false) String q, @ParameterObject Pageable pageable) {
        Page<PublicEvent> page = adminCatalogService.search(q, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/{id}")
    public PublicEvent findOne(@PathVariable Long id) {
        return adminCatalogService.findOne(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event " + id + " not found"));
    }

    /**
     * {@code POST /api/admin/events?publish=true} : create an event with its venue and ticket types; DRAFT unless published.
     */
    @PostMapping("")
    public ResponseEntity<PublicEvent> create(@Valid @RequestBody EventForm form, @RequestParam(defaultValue = "false") boolean publish) {
        PublicEvent event = adminCatalogService.create(form, publish);
        return ResponseEntity.created(URI.create("/api/admin/events/" + event.id())).body(event);
    }

    @PutMapping("/{id}")
    public PublicEvent update(@PathVariable Long id, @Valid @RequestBody EventForm form) {
        return adminCatalogService.update(id, form);
    }

    /**
     * {@code POST /api/admin/events/{id}/status} : publish, cancel or end an event.
     */
    @PostMapping("/{id}/status")
    public PublicEvent changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChange change) {
        return adminCatalogService.changeStatus(id, change.status());
    }

    public record StatusChange(@NotNull EventStatus status) {}
}
