package com.ticketrush.inventory.service.impl;

import com.ticketrush.inventory.domain.StockHold;
import com.ticketrush.inventory.repository.StockHoldRepository;
import com.ticketrush.inventory.service.StockHoldService;
import com.ticketrush.inventory.service.dto.StockHoldDTO;
import com.ticketrush.inventory.service.mapper.StockHoldMapper;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.ticketrush.inventory.domain.StockHold}.
 */
@Service
@Transactional
public class StockHoldServiceImpl implements StockHoldService {

    private static final Logger LOG = LoggerFactory.getLogger(StockHoldServiceImpl.class);

    private final StockHoldRepository stockHoldRepository;

    private final StockHoldMapper stockHoldMapper;

    public StockHoldServiceImpl(StockHoldRepository stockHoldRepository, StockHoldMapper stockHoldMapper) {
        this.stockHoldRepository = stockHoldRepository;
        this.stockHoldMapper = stockHoldMapper;
    }

    @Override
    public StockHoldDTO save(StockHoldDTO stockHoldDTO) {
        LOG.debug("Request to save StockHold : {}", stockHoldDTO);
        StockHold stockHold = stockHoldMapper.toEntity(stockHoldDTO);
        stockHold = stockHoldRepository.save(stockHold);
        return stockHoldMapper.toDto(stockHold);
    }

    @Override
    public StockHoldDTO update(StockHoldDTO stockHoldDTO) {
        LOG.debug("Request to update StockHold : {}", stockHoldDTO);
        StockHold stockHold = stockHoldMapper.toEntity(stockHoldDTO);
        stockHold = stockHoldRepository.save(stockHold);
        return stockHoldMapper.toDto(stockHold);
    }

    @Override
    public Optional<StockHoldDTO> partialUpdate(StockHoldDTO stockHoldDTO) {
        LOG.debug("Request to partially update StockHold : {}", stockHoldDTO);

        return stockHoldRepository
            .findById(stockHoldDTO.getId())
            .map(existingStockHold -> {
                stockHoldMapper.partialUpdate(existingStockHold, stockHoldDTO);

                return existingStockHold;
            })
            .map(stockHoldRepository::save)
            .map(stockHoldMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockHoldDTO> findAll() {
        LOG.debug("Request to get all StockHolds");
        return stockHoldRepository.findAll().stream().map(stockHoldMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockHoldDTO> findOne(Long id) {
        LOG.debug("Request to get StockHold : {}", id);
        return stockHoldRepository.findById(id).map(stockHoldMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete StockHold : {}", id);
        stockHoldRepository.deleteById(id);
    }
}
