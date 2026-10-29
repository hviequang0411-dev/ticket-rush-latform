package com.ticketrush.catalog.service.mapper;

import static com.ticketrush.catalog.domain.VenueAsserts.*;
import static com.ticketrush.catalog.domain.VenueTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VenueMapperTest {

    private VenueMapper venueMapper;

    @BeforeEach
    void setUp() {
        venueMapper = new VenueMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getVenueSample1();
        var actual = venueMapper.toEntity(venueMapper.toDto(expected));
        assertVenueAllPropertiesEquals(expected, actual);
    }
}
