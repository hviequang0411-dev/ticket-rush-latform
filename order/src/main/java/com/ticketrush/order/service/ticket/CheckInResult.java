package com.ticketrush.order.service.ticket;

/**
 * What the gate shows after a scan. A rejected ticket is a normal outcome, not an error: {@code ticket} is set
 * whenever the code was genuine, so staff can see who it belongs to and when it was used.
 */
public record CheckInResult(Outcome outcome, TicketView ticket) {
    public enum Outcome {
        /** Let them in. */
        ADMITTED,
        /** Already scanned (possibly a copy of the QR code). */
        ALREADY_USED,
        /** Voided with its order. */
        CANCELLED,
        /** Not a TicketRush code, edited, or unknown. */
        INVALID,
    }
}
