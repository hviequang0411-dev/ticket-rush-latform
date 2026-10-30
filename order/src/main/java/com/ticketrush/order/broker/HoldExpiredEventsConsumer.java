package com.ticketrush.order.broker;

import com.ticketrush.order.service.saga.OrderSaga;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Feeds seat hold expiries into the order saga. Bound to {@code holdExpired-in-0}.
 */
@Component("holdExpired")
public class HoldExpiredEventsConsumer implements Consumer<Message<HoldExpiredEvent>> {

    private static final Logger LOG = LoggerFactory.getLogger(HoldExpiredEventsConsumer.class);

    private final OrderSaga orderSaga;

    public HoldExpiredEventsConsumer(OrderSaga orderSaga) {
        this.orderSaga = orderSaga;
    }

    @Override
    public void accept(Message<HoldExpiredEvent> message) {
        LOG.debug("Received hold expired event : {}", message.getPayload());
        orderSaga.onHoldExpired(message.getPayload().orderId(), EventIds.of(message));
    }
}
