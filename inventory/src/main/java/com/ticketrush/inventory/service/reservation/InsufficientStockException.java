package com.ticketrush.inventory.service.reservation;

import java.io.Serial;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Not enough seats left for one of the requested ticket types. Nothing was held.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientStockException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InsufficientStockException(Long ticketTypeId, int requested) {
        super("Not enough seats left for ticket type " + ticketTypeId + " (requested " + requested + ")");
    }
}
