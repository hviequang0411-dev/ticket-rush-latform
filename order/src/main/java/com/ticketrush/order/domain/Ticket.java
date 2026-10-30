package com.ticketrush.order.domain;

import com.ticketrush.order.domain.enumeration.TicketStatus;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * One seat of a confirmed order. Written once when issued; afterwards only the check-in columns change, and only
 * through {@link com.ticketrush.order.repository.TicketRepository#checkIn}.
 */
@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", length = 32, nullable = false, unique = true, updatable = false)
    private String code;

    @Column(name = "order_id", nullable = false, updatable = false)
    private Long orderId;

    @Column(name = "order_code", length = 32, nullable = false, updatable = false)
    private String orderCode;

    @Column(name = "user_login", length = 100, nullable = false, updatable = false)
    private String userLogin;

    @Column(name = "event_id", nullable = false, updatable = false)
    private Long eventId;

    @Column(name = "ticket_type_id", nullable = false, updatable = false)
    private Long ticketTypeId;

    @Column(name = "seat_no", nullable = false, updatable = false)
    private Integer seatNo;

    @Column(name = "seats_of_type", nullable = false, updatable = false)
    private Integer seatsOfType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 16, nullable = false)
    private TicketStatus status;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "checked_in_by", length = 100)
    private String checkedInBy;

    protected Ticket() {}

    public Ticket(String code, Order order, OrderItem item, int seatNo, Instant issuedAt) {
        this.code = code;
        this.orderId = order.getId();
        this.orderCode = order.getCode();
        this.userLogin = order.getUserLogin();
        this.eventId = item.getEventId();
        this.ticketTypeId = item.getTicketTypeId();
        this.seatNo = seatNo;
        this.seatsOfType = item.getQuantity();
        this.status = TicketStatus.VALID;
        this.issuedAt = issuedAt;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public Long getEventId() {
        return eventId;
    }

    public Long getTicketTypeId() {
        return ticketTypeId;
    }

    public Integer getSeatNo() {
        return seatNo;
    }

    public Integer getSeatsOfType() {
        return seatsOfType;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public String getCheckedInBy() {
        return checkedInBy;
    }
}
