package com.ticketrush.catalog.service.mapper;

import com.ticketrush.catalog.domain.Event;
import com.ticketrush.catalog.domain.TicketType;
import com.ticketrush.catalog.service.dto.EventDTO;
import com.ticketrush.catalog.service.dto.TicketTypeDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link TicketType} and its DTO {@link TicketTypeDTO}.
 */
@Mapper(componentModel = "spring")
public interface TicketTypeMapper extends EntityMapper<TicketTypeDTO, TicketType> {
    @Mapping(target = "event", source = "event", qualifiedByName = "eventName")
    TicketTypeDTO toDto(TicketType s);

    @Named("eventName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    // Order checks these before selling: only published events, and the waiting room when it is on
    @Mapping(target = "status", source = "status")
    @Mapping(target = "waitingRoom", source = "waitingRoom")
    EventDTO toDtoEventName(Event event);
}
