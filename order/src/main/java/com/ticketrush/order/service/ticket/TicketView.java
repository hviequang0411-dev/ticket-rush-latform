package com.ticketrush.order.service.ticket;

import com.ticketrush.order.domain.Ticket;
import com.ticketrush.order.domain.enumeration.TicketStatus;
import java.time.Instant;

/**
 * A ticket as its holder and the gate staff see it, with the signed QR payload.
 */
public record TicketView(
    String code,
    String qrPayload,
    String orderCode,
    String userLogin,
    Long eventId,
    Long ticketTypeId,
    int seatNo,
    int seatsOfType,
    TicketStatus status,
    Instant issuedAt,
    Instant checkedInAt
) {
    static TicketView of(Ticket ticket, TicketCodes codes) {
        return new TicketView(
            ticket.getCode(),
            codes.qrPayload(ticket.getCode()),
            ticket.getOrderCode(),
            ticket.getUserLogin(),
            ticket.getEventId(),
            ticket.getTicketTypeId(),
            ticket.getSeatNo(),
            ticket.getSeatsOfType(),
            ticket.getStatus(),
            ticket.getIssuedAt(),
            ticket.getCheckedInAt()
        );
    }
}
