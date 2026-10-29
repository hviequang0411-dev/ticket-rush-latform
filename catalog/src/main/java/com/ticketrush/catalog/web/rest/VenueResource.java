package com.ticketrush.catalog.web.rest;

import com.ticketrush.catalog.repository.VenueRepository;
import com.ticketrush.catalog.service.VenueService;
import com.ticketrush.catalog.service.dto.VenueDTO;
import com.ticketrush.catalog.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.ticketrush.catalog.domain.Venue}.
 */
@RestController
@RequestMapping("/api/venues")
public class VenueResource {

    private static final Logger LOG = LoggerFactory.getLogger(VenueResource.class);

    private static final String ENTITY_NAME = "catalogVenue";

    @Value("${jhipster.clientApp.name:catalog}")
    private String applicationName;

    private final VenueService venueService;

    private final VenueRepository venueRepository;

    public VenueResource(VenueService venueService, VenueRepository venueRepository) {
        this.venueService = venueService;
        this.venueRepository = venueRepository;
    }

    /**
     * {@code POST  /venues} : Create a new venue.
     *
     * @param venueDTO the venueDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new venueDTO, or with status {@code 400 (Bad Request)} if the venue already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<VenueDTO> createVenue(@Valid @RequestBody VenueDTO venueDTO) throws URISyntaxException {
        LOG.debug("REST request to save Venue : {}", venueDTO);
        if (venueDTO.getId() != null) {
            throw new BadRequestAlertException("A new venue cannot already have an ID", ENTITY_NAME, "idexists");
        }
        venueDTO = venueService.save(venueDTO);
        return ResponseEntity.created(new URI("/api/venues/" + venueDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, venueDTO.getId().toString()))
            .body(venueDTO);
    }

    /**
     * {@code PUT  /venues/:id} : Updates an existing venue.
     *
     * @param id the id of the venueDTO to save.
     * @param venueDTO the venueDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated venueDTO,
     * or with status {@code 400 (Bad Request)} if the venueDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the venueDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<VenueDTO> updateVenue(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody VenueDTO venueDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update Venue : {}, {}", id, venueDTO);
        if (venueDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, venueDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!venueRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        venueDTO = venueService.update(venueDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, venueDTO.getId().toString()))
            .body(venueDTO);
    }

    /**
     * {@code PATCH  /venues/:id} : Partial updates given fields of an existing venue, field will ignore if it is null
     *
     * @param id the id of the venueDTO to save.
     * @param venueDTO the venueDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated venueDTO,
     * or with status {@code 400 (Bad Request)} if the venueDTO is not valid,
     * or with status {@code 404 (Not Found)} if the venueDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the venueDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<VenueDTO> partialUpdateVenue(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody VenueDTO venueDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update Venue : {}, {}", id, venueDTO);
        if (venueDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, venueDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!venueRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<VenueDTO> result = venueService.partialUpdate(venueDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, venueDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /venues} : get all the Venues.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Venues in body.
     */
    @GetMapping("")
    public ResponseEntity<List<VenueDTO>> getAllVenues(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        LOG.debug("REST request to get a page of Venues");
        Page<VenueDTO> page = venueService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /venues/:id} : get the "id" venue.
     *
     * @param id the id of the venueDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the venueDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<VenueDTO> getVenue(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Venue : {}", id);
        Optional<VenueDTO> venueDTO = venueService.findOne(id);
        return ResponseUtil.wrapOrNotFound(venueDTO);
    }

    /**
     * {@code DELETE  /venues/:id} : delete the "id" venue.
     *
     * @param id the id of the venueDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVenue(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Venue : {}", id);
        venueService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
