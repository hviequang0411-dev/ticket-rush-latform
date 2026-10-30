package com.ticketrush.order.service.checkout;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The event sells through the waiting room and the request carried no valid admission pass for it.
 */
@ResponseStatus(HttpStatus.PRECONDITION_REQUIRED)
public class QueueRequiredException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public QueueRequiredException(Long eventId) {
        super("Event " + eventId + " sells through the waiting room: queue first");
    }
}
