package com.ticketrush.catalog.service.impl;

import com.ticketrush.catalog.domain.TicketType;
import com.ticketrush.catalog.repository.TicketTypeRepository;
import com.ticketrush.catalog.service.TicketTypeService;
import com.ticketrush.catalog.service.dto.TicketTypeDTO;
import com.ticketrush.catalog.service.event.TicketTypeChanged;
import com.ticketrush.catalog.service.mapper.TicketTypeMapper;
import com.ticketrush.catalog.service.outbox.Outbox;
import java.time.Clock;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.ticketrush.catalog.domain.TicketType}.
 */
@Service
@Transactional
public class TicketTypeServiceImpl implements TicketTypeService {

    private static final Logger LOG = LoggerFactory.getLogger(TicketTypeServiceImpl.class);

    private final TicketTypeRepository ticketTypeRepository;

    private final TicketTypeMapper ticketTypeMapper;

    private final Outbox outbox;

    private final Clock clock;

    public TicketTypeServiceImpl(
        TicketTypeRepository ticketTypeRepository,
        TicketTypeMapper ticketTypeMapper,
        Outbox outbox,
        Clock clock
    ) {
        this.ticketTypeRepository = ticketTypeRepository;
        this.ticketTypeMapper = ticketTypeMapper;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Override
    public TicketTypeDTO save(TicketTypeDTO ticketTypeDTO) {
        LOG.debug("Request to save TicketType : {}", ticketTypeDTO);
        TicketType ticketType = ticketTypeMapper.toEntity(ticketTypeDTO);
        ticketType = ticketTypeRepository.save(ticketType);
        raise(TicketTypeChanged.Type.CREATED, ticketType);
        return ticketTypeMapper.toDto(ticketType);
    }

    @Override
    public TicketTypeDTO update(TicketTypeDTO ticketTypeDTO) {
        LOG.debug("Request to update TicketType : {}", ticketTypeDTO);
        TicketType ticketType = ticketTypeMapper.toEntity(ticketTypeDTO);
        ticketType = ticketTypeRepository.save(ticketType);
        raise(TicketTypeChanged.Type.UPDATED, ticketType);
        return ticketTypeMapper.toDto(ticketType);
    }

    @Override
    public Optional<TicketTypeDTO> partialUpdate(TicketTypeDTO ticketTypeDTO) {
        LOG.debug("Request to partially update TicketType : {}", ticketTypeDTO);

        return ticketTypeRepository
            .findById(ticketTypeDTO.getId())
            .map(existingTicketType -> {
                ticketTypeMapper.partialUpdate(existingTicketType, ticketTypeDTO);

                return existingTicketType;
            })
            .map(ticketTypeRepository::save)
            .map(ticketType -> {
                raise(TicketTypeChanged.Type.UPDATED, ticketType);
                return ticketType;
            })
            .map(ticketTypeMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketTypeDTO> findAll() {
        LOG.debug("Request to get all TicketTypes");
        return ticketTypeRepository.findAll().stream().map(ticketTypeMapper::toDto).collect(Collectors.toCollection(LinkedList::new));
    }

    public List<TicketTypeDTO> findAllWithEagerRelationships() {
        return ticketTypeRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(ticketTypeMapper::toDto)
            .collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TicketTypeDTO> findOne(Long id) {
        LOG.debug("Request to get TicketType : {}", id);
        return ticketTypeRepository.findOneWithEagerRelationships(id).map(ticketTypeMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete TicketType : {}", id);
        ticketTypeRepository.deleteById(id);
    }

    private void raise(TicketTypeChanged.Type type, TicketType ticketType) {
        Long eventId = ticketType.getEvent() != null ? ticketType.getEvent().getId() : null;
        outbox.append(
            TicketTypeChanged.TOPIC,
            "TicketType",
            ticketType.getId(),
            type.name(),
            new TicketTypeChanged(type, ticketType.getId(), eventId, ticketType.getTotalQuantity(), clock.instant())
        );
    }
}
