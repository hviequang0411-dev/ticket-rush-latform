package com.ticketrush.inventory.domain;

import static com.ticketrush.inventory.domain.StockHoldTestSamples.*;
import static com.ticketrush.inventory.domain.TicketStockTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.ticketrush.inventory.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class StockHoldTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(StockHold.class);
        StockHold stockHold1 = getStockHoldSample1();
        StockHold stockHold2 = new StockHold();
        assertThat(stockHold1).isNotEqualTo(stockHold2);

        stockHold2.setId(stockHold1.getId());
        assertThat(stockHold1).isEqualTo(stockHold2);

        stockHold2 = getStockHoldSample2();
        assertThat(stockHold1).isNotEqualTo(stockHold2);
    }

    @Test
    void stockTest() {
        StockHold stockHold = getStockHoldRandomSampleGenerator();
        TicketStock ticketStockBack = getTicketStockRandomSampleGenerator();

        stockHold.setStock(ticketStockBack);
        assertThat(stockHold.getStock()).isEqualTo(ticketStockBack);

        stockHold.stock(null);
        assertThat(stockHold.getStock()).isNull();
    }
}
