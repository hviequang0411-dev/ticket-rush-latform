package com.ticketrush.payment.service.flow;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A callback whose signature does not verify. It is stored for auditing but never applied.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidWebhookException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidWebhookException(String message) {
        super(message);
    }
}
