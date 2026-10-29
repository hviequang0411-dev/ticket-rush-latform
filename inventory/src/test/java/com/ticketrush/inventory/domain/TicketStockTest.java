package com.ticketrush.inventory.domain;

import static com.ticketrush.inventory.domain.TicketStockTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.ticketrush.inventory.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class TicketStockTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(TicketStock.class);
        TicketStock ticketStock1 = getTicketStockSample1();
        TicketStock ticketStock2 = new TicketStock();
        assertThat(ticketStock1).isNotEqualTo(ticketStock2);

        ticketStock2.setId(ticketStock1.getId());
        assertThat(ticketStock1).isEqualTo(ticketStock2);

        ticketStock2 = getTicketStockSample2();
        assertThat(ticketStock1).isNotEqualTo(ticketStock2);
    }
}
