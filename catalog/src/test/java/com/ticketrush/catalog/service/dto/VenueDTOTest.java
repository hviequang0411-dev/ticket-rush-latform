package com.ticketrush.catalog.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketrush.catalog.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class VenueDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(VenueDTO.class);
        VenueDTO venueDTO1 = new VenueDTO();
        venueDTO1.setId(1L);
        VenueDTO venueDTO2 = new VenueDTO();
        assertThat(venueDTO1).isNotEqualTo(venueDTO2);
        venueDTO2.setId(venueDTO1.getId());
        assertThat(venueDTO1).isEqualTo(venueDTO2);
        venueDTO2.setId(2L);
        assertThat(venueDTO1).isNotEqualTo(venueDTO2);
        venueDTO1.setId(null);
        assertThat(venueDTO1).isNotEqualTo(venueDTO2);
    }
}
