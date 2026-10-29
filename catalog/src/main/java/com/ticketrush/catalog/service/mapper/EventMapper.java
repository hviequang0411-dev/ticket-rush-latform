package com.ticketrush.catalog.service.mapper;

import com.ticketrush.catalog.domain.Event;
import com.ticketrush.catalog.domain.Venue;
import com.ticketrush.catalog.service.dto.EventDTO;
import com.ticketrush.catalog.service.dto.VenueDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Event} and its DTO {@link EventDTO}.
 */
@Mapper(componentModel = "spring")
public interface EventMapper extends EntityMapper<EventDTO, Event> {
    @Mapping(target = "venue", source = "venue", qualifiedByName = "venueName")
    EventDTO toDto(Event s);

    @Named("venueName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    VenueDTO toDtoVenueName(Venue venue);
}
