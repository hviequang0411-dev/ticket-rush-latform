package com.ticketrush.payment.service.flow;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The order's payment is not in a state that allows the request, e.g. cancelling a payment that already succeeded.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class PaymentStateException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PaymentStateException(String message) {
        super(message);
    }
}
