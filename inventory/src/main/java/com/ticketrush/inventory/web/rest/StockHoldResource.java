package com.ticketrush.inventory.web.rest;

import com.ticketrush.inventory.repository.StockHoldRepository;
import com.ticketrush.inventory.service.StockHoldService;
import com.ticketrush.inventory.service.dto.StockHoldDTO;
import com.ticketrush.inventory.web.rest.errors.BadRequestAlertException;
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
 * REST controller for managing {@link com.ticketrush.inventory.domain.StockHold}.
 */
@RestController
@RequestMapping("/api/stock-holds")
public class StockHoldResource {

    private static final Logger LOG = LoggerFactory.getLogger(StockHoldResource.class);

    private static final String ENTITY_NAME = "inventoryStockHold";

    @Value("${jhipster.clientApp.name:inventory}")
    private String applicationName;

    private final StockHoldService stockHoldService;

    private final StockHoldRepository stockHoldRepository;

    public StockHoldResource(StockHoldService stockHoldService, StockHoldRepository stockHoldRepository) {
        this.stockHoldService = stockHoldService;
        this.stockHoldRepository = stockHoldRepository;
    }

    /**
     * {@code POST  /stock-holds} : Create a new stockHold.
     *
     * @param stockHoldDTO the stockHoldDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new stockHoldDTO, or with status {@code 400 (Bad Request)} if the stockHold already has an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<StockHoldDTO> createStockHold(@Valid @RequestBody StockHoldDTO stockHoldDTO) throws URISyntaxException {
        LOG.debug("REST request to save StockHold : {}", stockHoldDTO);
        if (stockHoldDTO.getId() != null) {
            throw new BadRequestAlertException("A new stockHold cannot already have an ID", ENTITY_NAME, "idexists");
        }
        stockHoldDTO = stockHoldService.save(stockHoldDTO);
        return ResponseEntity.created(new URI("/api/stock-holds/" + stockHoldDTO.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, stockHoldDTO.getId().toString()))
            .body(stockHoldDTO);
    }

    /**
     * {@code PUT  /stock-holds/:id} : Updates an existing stockHold.
     *
     * @param id the id of the stockHoldDTO to save.
     * @param stockHoldDTO the stockHoldDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated stockHoldDTO,
     * or with status {@code 400 (Bad Request)} if the stockHoldDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the stockHoldDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public ResponseEntity<StockHoldDTO> updateStockHold(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody StockHoldDTO stockHoldDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to update StockHold : {}, {}", id, stockHoldDTO);
        if (stockHoldDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, stockHoldDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!stockHoldRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        stockHoldDTO = stockHoldService.update(stockHoldDTO);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, stockHoldDTO.getId().toString()))
            .body(stockHoldDTO);
    }

    /**
     * {@code PATCH  /stock-holds/:id} : Partial updates given fields of an existing stockHold, field will ignore if it is null
     *
     * @param id the id of the stockHoldDTO to save.
     * @param stockHoldDTO the stockHoldDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated stockHoldDTO,
     * or with status {@code 400 (Bad Request)} if the stockHoldDTO is not valid,
     * or with status {@code 404 (Not Found)} if the stockHoldDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the stockHoldDTO couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<StockHoldDTO> partialUpdateStockHold(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody StockHoldDTO stockHoldDTO
    ) throws URISyntaxException {
        LOG.debug("REST request to partially update StockHold : {}, {}", id, stockHoldDTO);
        if (stockHoldDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, stockHoldDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!stockHoldRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<StockHoldDTO> result = stockHoldService.partialUpdate(stockHoldDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, stockHoldDTO.getId().toString())
        );
    }

    /**
     * {@code GET  /stock-holds} : get all the Stock Holds.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of Stock Holds in body.
     */
    @GetMapping("")
    public List<StockHoldDTO> getAllStockHolds() {
        LOG.debug("REST request to get all StockHolds");
        return stockHoldService.findAll();
    }

    /**
     * {@code GET  /stock-holds/:id} : get the "id" stockHold.
     *
     * @param id the id of the stockHoldDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the stockHoldDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<StockHoldDTO> getStockHold(@PathVariable("id") Long id) {
        LOG.debug("REST request to get StockHold : {}", id);
        Optional<StockHoldDTO> stockHoldDTO = stockHoldService.findOne(id);
        return ResponseUtil.wrapOrNotFound(stockHoldDTO);
    }

    /**
     * {@code DELETE  /stock-holds/:id} : delete the "id" stockHold.
     *
     * @param id the id of the stockHoldDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStockHold(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete StockHold : {}", id);
        stockHoldService.delete(id);
        return ResponseEntity.noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }
}
