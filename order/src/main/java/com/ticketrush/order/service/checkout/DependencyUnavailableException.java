package com.ticketrush.order.service.checkout;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Catalog or inventory did not answer. Safe to retry with the same Idempotency-Key.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class DependencyUnavailableException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DependencyUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
