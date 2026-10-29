package com.ticketrush.inventory.service.mapper;

import static com.ticketrush.inventory.domain.StockHoldAsserts.*;
import static com.ticketrush.inventory.domain.StockHoldTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StockHoldMapperTest {

    private StockHoldMapper stockHoldMapper;

    @BeforeEach
    void setUp() {
        stockHoldMapper = new StockHoldMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getStockHoldSample1();
        var actual = stockHoldMapper.toEntity(stockHoldMapper.toDto(expected));
        assertStockHoldAllPropertiesEquals(expected, actual);
    }
}
