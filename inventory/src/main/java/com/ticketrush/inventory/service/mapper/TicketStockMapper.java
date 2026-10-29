package com.ticketrush.inventory.service.mapper;

import com.ticketrush.inventory.domain.TicketStock;
import com.ticketrush.inventory.service.dto.TicketStockDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link TicketStock} and its DTO {@link TicketStockDTO}.
 */
@Mapper(componentModel = "spring")
public interface TicketStockMapper extends EntityMapper<TicketStockDTO, TicketStock> {}
