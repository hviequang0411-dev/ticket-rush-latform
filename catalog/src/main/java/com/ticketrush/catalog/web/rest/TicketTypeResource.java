package com.ticketrush.catalog.web.rest;

import com.ticketrush.catalog.repository.TicketTypeRepository;
import com.ticketrush.catalog.service.TicketTypeService;
import com.ticketrush.catalog.service.dto.TicketTypeDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.ticketrush.catalog.domain.TicketType}.
 */
@RestController
@RequestMapping("/api/ticket-types")
public class TicketTypeResource {

    private static final Logger LOG = LoggerFactory.getLogger(TicketTypeResource.class);

    private static final String ENTITY_NAME = "catalogTicketType";

    @Value("${jhipster.clientApp.name:catalog}")
    private String applicationName;

    private final TicketTypeService ticketTypeService;

    private final TicketTypeRepository ticketTypeRepository;

    public TicketTypeResource(TicketTypeService ticketTypeService, TicketTypeRepository ticketTypeRepository) {
        this.ticketTypeService = ticketTypeService;
        this.ticketTypeRepository = ticketTypeRepository;
    }

    /**
     * {@code POST  /ticket-types} : Create a new ticketType.
     *
     * @param ticketTypeDTO the ticketTypeDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new ticketTypeDTO, or with status {@code 400 (Bad Request)} if the ticketType already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<TicketTypeDTO> createTicketType(@Valid @RequestBody TicketTypeDTO ticketTypeDTO) throws URISyntaxException {
        LOG.debug("REST request to save TicketType : {}", ticketTypeDTO);
        if (ticketTypeDTO.getId() != null) {
            throw new BadRequestAlertException("A new ticketType cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ticketTypeDTO = ticketTypeService.save(ticketTypeDTO);
        return ResponseEntity.created(new URI("/api/ticket-types/" + ticketTypeDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, ticketTypeDTO.getId().toString()))
            .body(ticketTypeDTO);
    }

    /**
     * {@code PUT  /ticket-types/:id} : Updates an existing ticketType.
     *
     * @param id the id of the ticketTypeDTO to save.
     * @param ticketTypeDTO the ticketTypeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated ticketTypeDTO,
     * or with status {@code 400 (Bad Request)} if the ticketTypeDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the ticketTypeDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TicketTypeDTO> updateTicketType(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody TicketTypeDTO ticketTypeDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update TicketType : {}, {}", id, ticketTypeDTO);
        if (ticketTypeDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, ticketTypeDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!ticketTypeRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        ticketTypeDTO = ticketTypeService.update(ticketTypeDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, ticketTypeDTO.getId().toString()))
            .body(ticketTypeDTO);
    }

    /**
     * {@code PATCH  /ticket-types/:id} : Partial updates given fields of an existing ticketType, field will ignore if it is null
     *
     * @param id the id of the ticketTypeDTO to save.
     * @param ticketTypeDTO the ticketTypeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated ticketTypeDTO,
     * or with status {@code 400 (Bad Request)} if the ticketTypeDTO is not valid,
     * or with status {@code 404 (Not Found)} if the ticketTypeDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the ticketTypeDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<TicketTypeDTO> partialUpdateTicketType(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody TicketTypeDTO ticketTypeDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update TicketType : {}, {}", id, ticketTypeDTO);
        if (ticketTypeDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, ticketTypeDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!ticketTypeRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<TicketTypeDTO> result = ticketTypeService.partialUpdate(ticketTypeDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, ticketTypeDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /ticket-types} : get all the Ticket Types.
     *
     * @param eagerload flag to eager load entities from relationships (This is applicable for many-to-many).
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Ticket Types in body.
     */
    @GetMapping("")
    public List<TicketTypeDTO> getAllTicketTypes(
        @RequestParam(name = "eagerload", required = false, defaultValue = "true") boolean eagerload
    ) {
        LOG.debug("REST request to get all TicketTypes");
        if (eagerload) {
            return ticketTypeService.findAllWithEagerRelationships();
        } else {
            return ticketTypeService.findAll();
        }
    }

    /**
     * {@code GET  /ticket-types/:id} : get the "id" ticketType.
     *
     * @param id the id of the ticketTypeDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the ticketTypeDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TicketTypeDTO> getTicketType(@PathVariable("id") Long id) {
        LOG.debug("REST request to get TicketType : {}", id);
        Optional<TicketTypeDTO> ticketTypeDTO = ticketTypeService.findOne(id);
        return ResponseUtil.wrapOrNotFound(ticketTypeDTO);
    }

    /**
     * {@code DELETE  /ticket-types/:id} : delete the "id" ticketType.
     *
     * @param id the id of the ticketTypeDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicketType(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete TicketType : {}", id);
        ticketTypeService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
