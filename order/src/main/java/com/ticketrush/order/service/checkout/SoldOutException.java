package com.ticketrush.order.service.checkout;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Not enough seats left. The order is kept as FAILED so a retry with the same Idempotency-Key gets the same answer.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class SoldOutException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public SoldOutException(String orderCode) {
        super("Sold out (order " + orderCode + ")");
    }
}
