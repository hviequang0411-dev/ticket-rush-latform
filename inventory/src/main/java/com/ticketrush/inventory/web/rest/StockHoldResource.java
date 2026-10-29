package com.ticketrush.inventory.web.rest;

import com.ticketrush.inventory.service.StockHoldService;
import com.ticketrush.inventory.service.dto.StockHoldDTO;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for reading {@link com.ticketrush.inventory.domain.StockHold}.
 * <p>
 * Read-only on purpose: a hold written directly would bypass the stock accounting in ReservationService.
 */
@RestController
@RequestMapping("/api/stock-holds")
public class StockHoldResource {

    private static final Logger LOG = LoggerFactory.getLogger(StockHoldResource.class);

    private final StockHoldService stockHoldService;

    public StockHoldResource(StockHoldService stockHoldService) {
        this.stockHoldService = stockHoldService;
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
}
