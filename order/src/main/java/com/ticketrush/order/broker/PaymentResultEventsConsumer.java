package com.ticketrush.order.broker;

import com.ticketrush.order.service.saga.OrderSaga;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Feeds payment results into the order saga. Bound to {@code paymentResults-in-0}.
 */
@Component("paymentResults")
public class PaymentResultEventsConsumer implements Consumer<Message<PaymentResultEvent>> {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentResultEventsConsumer.class);

    private final OrderSaga orderSaga;

    public PaymentResultEventsConsumer(OrderSaga orderSaga) {
        this.orderSaga = orderSaga;
    }

    @Override
    public void accept(Message<PaymentResultEvent> message) {
        LOG.debug("Received payment result : {}", message.getPayload());
        orderSaga.onPaymentResult(message.getPayload(), EventIds.of(message));
    }
}
