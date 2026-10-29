package com.ticketrush.inventory.service.impl;

import com.ticketrush.inventory.domain.TicketStock;
import com.ticketrush.inventory.repository.TicketStockRepository;
import com.ticketrush.inventory.service.TicketStockService;
import com.ticketrush.inventory.service.dto.TicketStockDTO;
import com.ticketrush.inventory.service.mapper.TicketStockMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.ticketrush.inventory.domain.TicketStock}.
 */
@Service
@Transactional
public class TicketStockServiceImpl implements TicketStockService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketStockServiceImpl.class);

    private final TicketStockRepository ticketStockRepository;

    private final TicketStockMapper ticketStockMapper;

    public TicketStockServiceImpl(TicketStockRepository ticketStockRepository, TicketStockMapper ticketStockMapper) {
        this.ticketStockRepository = ticketStockRepository;
        this.ticketStockMapper = ticketStockMapper;
    }

    @Override
    public TicketStockDTO save(TicketStockDTO ticketStockDTO) {
        LOG.debug("Request to save TicketStock : {}", ticketStockDTO);
        TicketStock ticketStock = ticketStockMapper.toEntity(ticketStockDTO);
        ticketStock = ticketStockRepository.save(ticketStock);
        return ticketStockMapper.toDto(ticketStock);
    }

    @Override
    public TicketStockDTO update(TicketStockDTO ticketStockDTO) {
        LOG.debug("Request to update TicketStock : {}", ticketStockDTO);
        TicketStock ticketStock = ticketStockMapper.toEntity(ticketStockDTO);
        ticketStock = ticketStockRepository.save(ticketStock);
        return ticketStockMapper.toDto(ticketStock);
    }

    @Override
    public Optional<TicketStockDTO> partialUpdate(TicketStockDTO ticketStockDTO) {
        LOG.debug("Request to partially update TicketStock : {}", ticketStockDTO);

        return ticketStockRepository
            .findById(ticketStockDTO.getId())
            .map(existingTicketStock -> {
                ticketStockMapper.partialUpdate(existingTicketStock, ticketStockDTO);

                return existingTicketStock;
            })
            .map(ticketStockRepository::save)
            .map(ticketStockMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketStockDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all TicketStocks");
        return ticketStockRepository.findAll(pageable).map(ticketStockMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TicketStockDTO> findOne(Long id) {
        LOG.debug("Request to get TicketStock : {}", id);
        return ticketStockRepository.findById(id).map(ticketStockMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete TicketStock : {}", id);
        ticketStockRepository.deleteById(id);
    }
}
