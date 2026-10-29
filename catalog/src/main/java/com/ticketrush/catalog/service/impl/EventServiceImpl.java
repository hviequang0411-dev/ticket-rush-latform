package com.ticketrush.catalog.service.impl;

import com.ticketrush.catalog.domain.Event;
import com.ticketrush.catalog.repository.EventRepository;
import com.ticketrush.catalog.service.EventService;
import com.ticketrush.catalog.service.dto.EventDTO;
import com.ticketrush.catalog.service.mapper.EventMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link com.ticketrush.catalog.domain.Event}.
 */
@Service
@Transactional
public class EventServiceImpl implements EventService {

    private static final Logger LOG = LoggerFactory.getLogger(EventServiceImpl.class);

    private final EventRepository eventRepository;

    private final EventMapper eventMapper;

    public EventServiceImpl(EventRepository eventRepository, EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    public EventDTO save(EventDTO eventDTO) {
        LOG.debug("Request to save Event : {}", eventDTO);
        Event event = eventMapper.toEntity(eventDTO);
        event = eventRepository.save(event);
        return eventMapper.toDto(event);
    }

    @Override
    public EventDTO update(EventDTO eventDTO) {
        LOG.debug("Request to update Event : {}", eventDTO);
        Event event = eventMapper.toEntity(eventDTO);
        event = eventRepository.save(event);
        return eventMapper.toDto(event);
    }

    @Override
    public Optional<EventDTO> partialUpdate(EventDTO eventDTO) {
        LOG.debug("Request to partially update Event : {}", eventDTO);

        return eventRepository
            .findById(eventDTO.getId())
            .map(existingEvent -> {
                eventMapper.partialUpdate(existingEvent, eventDTO);

                return existingEvent;
            })
            .map(eventRepository::save)
            .map(eventMapper::toDto);
    }

    public Page<EventDTO> findAllWithEagerRelationships(Pageable pageable) {
        return eventRepository.findAllWithEagerRelationships(pageable).map(eventMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EventDTO> findOne(Long id) {
        LOG.debug("Request to get Event : {}", id);
        return eventRepository.findOneWithEagerRelationships(id).map(eventMapper::toDto);
    }

    @Override
    public void delete(Long id) {
        LOG.debug("Request to delete Event : {}", id);
        eventRepository.deleteById(id);
    }
}
