package com.ticketrush.catalog.service.mapper;

import com.ticketrush.catalog.domain.Venue;
import com.ticketrush.catalog.service.dto.VenueDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Venue} and its DTO {@link VenueDTO}.
 */
@Mapper(componentModel = "spring")
public interface VenueMapper extends EntityMapper<VenueDTO, Venue> {}
