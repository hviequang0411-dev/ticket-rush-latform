package com.ticketrush.order.domain.enumeration;

/**
 * VALID until scanned at the gate (USED) or voided with its order (CANCELLED).
 */
public enum TicketStatus {
    VALID,
    USED,
    CANCELLED,
}
