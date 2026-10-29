package com.ticketrush.inventory.service.mapper;

import static com.ticketrush.inventory.domain.TicketStockAsserts.*;
import static com.ticketrush.inventory.domain.TicketStockTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketStockMapperTest {

    private TicketStockMapper ticketStockMapper;

    @BeforeEach
    void setUp() {
        ticketStockMapper = new TicketStockMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getTicketStockSample1();
        var actual = ticketStockMapper.toEntity(ticketStockMapper.toDto(expected));
        assertTicketStockAllPropertiesEquals(expected, actual);
    }
}
