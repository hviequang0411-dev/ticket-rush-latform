package com.ticketrush.order.service.checkout;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The order is not in a state that allows the operation, or the Idempotency-Key belongs to someone else.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class OrderStateException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public OrderStateException(String message) {
        super(message);
    }
}
