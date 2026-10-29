package com.ticketrush.inventory.service.mapper;

import com.ticketrush.inventory.domain.StockHold;
import com.ticketrush.inventory.domain.TicketStock;
import com.ticketrush.inventory.service.dto.StockHoldDTO;
import com.ticketrush.inventory.service.dto.TicketStockDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link StockHold} and its DTO {@link StockHoldDTO}.
 */
@Mapper(componentModel = "spring")
public interface StockHoldMapper extends EntityMapper<StockHoldDTO, StockHold> {
    @Mapping(target = "stock", source = "stock", qualifiedByName = "ticketStockId")
    StockHoldDTO toDto(StockHold s);

    @Named("ticketStockId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    TicketStockDTO toDtoTicketStockId(TicketStock ticketStock);
}
