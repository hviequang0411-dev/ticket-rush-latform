package com.ticketrush.inventory.service.reservation;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * The order's holds are not in a state that allows the requested operation, for example confirming holds that
 * already expired, or reserving again with different items.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ReservationConflictException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public ReservationConflictException(String message) {
        super(message);
    }
}
