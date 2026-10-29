package com.ticketrush.inventory.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketrush.inventory.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class StockHoldDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(StockHoldDTO.class);
        StockHoldDTO stockHoldDTO1 = new StockHoldDTO();
        stockHoldDTO1.setId(1L);
        StockHoldDTO stockHoldDTO2 = new StockHoldDTO();
        assertThat(stockHoldDTO1).isNotEqualTo(stockHoldDTO2);
        stockHoldDTO2.setId(stockHoldDTO1.getId());
        assertThat(stockHoldDTO1).isEqualTo(stockHoldDTO2);
        stockHoldDTO2.setId(2L);
        assertThat(stockHoldDTO1).isNotEqualTo(stockHoldDTO2);
        stockHoldDTO1.setId(null);
        assertThat(stockHoldDTO1).isNotEqualTo(stockHoldDTO2);
    }
}
