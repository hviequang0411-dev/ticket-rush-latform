package com.ticketrush.order.domain;

import static com.ticketrush.order.domain.OrderItemTestSamples.*;
import static com.ticketrush.order.domain.OrderTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.ticketrush.order.web.rest.TestUtil;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Order.class);
        Order order1 = getOrderSample1();
        Order order2 = new Order();
        assertThat(order1).isNotEqualTo(order2);

        order2.setId(order1.getId());
        assertThat(order1).isEqualTo(order2);

        order2 = getOrderSample2();
        assertThat(order1).isNotEqualTo(order2);
    }

    @Test
    void itemsTest() {
        Order order = getOrderRandomSampleGenerator();
        OrderItem orderItemBack = getOrderItemRandomSampleGenerator();

        order.addItems(orderItemBack);
        assertThat(order.getItemses()).containsOnly(orderItemBack);
        assertThat(orderItemBack.getOrder()).isEqualTo(order);

        order.removeItems(orderItemBack);
        assertThat(order.getItemses()).doesNotContain(orderItemBack);
        assertThat(orderItemBack.getOrder()).isNull();

        order.itemses(new HashSet<>(Set.of(orderItemBack)));
        assertThat(order.getItemses()).containsOnly(orderItemBack);
        assertThat(orderItemBack.getOrder()).isEqualTo(order);

        order.setItemses(new HashSet<>());
        assertThat(order.getItemses()).doesNotContain(orderItemBack);
        assertThat(orderItemBack.getOrder()).isNull();
    }
}
