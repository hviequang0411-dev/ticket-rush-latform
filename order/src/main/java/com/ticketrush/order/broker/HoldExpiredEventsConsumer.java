package com.ticketrush.order.broker;

import com.ticketrush.order.service.checkout.CheckoutService;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Expires orders whose seat hold ran out. Bound to {@code holdExpired-in-0}.
 */
@Component("holdExpired")
public class HoldExpiredEventsConsumer implements Consumer<HoldExpiredEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(HoldExpiredEventsConsumer.class);

    private final CheckoutService checkoutService;

    public HoldExpiredEventsConsumer(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @Override
    public void accept(HoldExpiredEvent event) {
        LOG.debug("Received hold expired event : {}", event);
        checkoutService.expire(event.orderId());
    }
}
