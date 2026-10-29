package com.ticketrush.inventory.broker;

import com.ticketrush.inventory.service.reservation.ExpiredHold;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

/**
 * Sends hold events to Kafka.
 * <p>
 * Called after the expiry transaction committed. If the process dies in between, the event is lost while the seats
 * are already back on sale; the order then stays RESERVED until payment fails. Phase 3 replaces this with a
 * transactional outbox.
 */
@Component
public class HoldEventsPublisher {

    static final String HOLD_EXPIRED_BINDING = "holdExpired-out-0";

    private static final Logger LOG = LoggerFactory.getLogger(HoldEventsPublisher.class);

    private final StreamBridge streamBridge;

    public HoldEventsPublisher(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    public void publishExpired(Collection<ExpiredHold> holds) {
        Map<Long, ExpiredHold> byOrder = new LinkedHashMap<>();
        holds.forEach(hold -> byOrder.putIfAbsent(hold.orderId(), hold));
        byOrder
            .values()
            .forEach(hold -> {
                if (!streamBridge.send(HOLD_EXPIRED_BINDING, new HoldExpiredEvent(hold.orderId(), hold.expiredAt()))) {
                    LOG.error("Could not publish hold expiry of order {}", hold.orderId());
                }
            });
    }
}
