package com.ticketrush.inventory.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.ticketrush.inventory.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class TicketStockDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(TicketStockDTO.class);
        TicketStockDTO ticketStockDTO1 = new TicketStockDTO();
        ticketStockDTO1.setId(1L);
        TicketStockDTO ticketStockDTO2 = new TicketStockDTO();
        assertThat(ticketStockDTO1).isNotEqualTo(ticketStockDTO2);
        ticketStockDTO2.setId(ticketStockDTO1.getId());
        assertThat(ticketStockDTO1).isEqualTo(ticketStockDTO2);
        ticketStockDTO2.setId(2L);
        assertThat(ticketStockDTO1).isNotEqualTo(ticketStockDTO2);
        ticketStockDTO1.setId(null);
        assertThat(ticketStockDTO1).isNotEqualTo(ticketStockDTO2);
    }
}
