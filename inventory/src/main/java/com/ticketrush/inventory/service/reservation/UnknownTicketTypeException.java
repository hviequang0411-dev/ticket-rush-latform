package com.ticketrush.inventory.service.reservation;

import java.io.Serial;
import java.util.Collection;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * A requested ticket type has no stock record, either it does not exist or catalog has not published it yet.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class UnknownTicketTypeException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public UnknownTicketTypeException(Collection<Long> ticketTypeIds) {
        super("No stock for ticket types " + ticketTypeIds);
    }
}
