package com.ticketrush.order.service.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * The organiser's dashboard numbers. Days are local days in {@code zone}.
 *
 * @param activeHolds orders holding seats right now (reserved or awaiting payment).
 * @param pace seats confirmed per event over the last {@code paceMinutes} minutes.
 */
public record SalesStats(
    String zone,
    Day today,
    Day yesterday,
    long activeHolds,
    List<DailyRevenue> daily,
    int paceMinutes,
    List<EventPace> pace
) {
    /**
     * @param revenue confirmed on that day.
     * @param ordersPlaced orders created that day, any outcome.
     * @param ordersConfirmed of those, how many are confirmed.
     * @param ordersFinished of those, how many reached a final state; the success rate is confirmed / finished.
     */
    public record Day(LocalDate date, BigDecimal revenue, long ordersPlaced, long ordersConfirmed, long ordersFinished) {}

    public record DailyRevenue(LocalDate date, BigDecimal revenue, long orders) {}

    public record EventPace(Long eventId, long seats) {}
}
