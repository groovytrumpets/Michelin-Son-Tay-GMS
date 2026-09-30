package com.g42.platform.gms.dashboard.infrastructure;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Đọc / ghi bảng cache {@code dashboard_daily_summary} (changeset 046) — mỗi ngày một dòng.
 * Dùng JDBC thay vì JPA vì chỉ cần đọc theo khoảng ngày và upsert hàng loạt: kỳ "Tất cả" lần
 * đầu có thể phải ghi vài nghìn dòng một lúc.
 */
@Repository
@RequiredArgsConstructor
public class DashboardDailySummaryStore {

    private static final String COLUMNS = "summary_date, paid_revenue, paid_bill_count, discount_amount,"
            + " legacy_revenue, legacy_visit_count, new_customers, new_customers_male, new_customers_female,"
            + " new_customers_other, booking_requests, bookings_scheduled, bookings_done, bookings_not_arrived,"
            + " bookings_cancelled, tickets_completed, computed_at";

    private static final String UPSERT = "INSERT INTO dashboard_daily_summary (" + COLUMNS + ")"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            + " ON DUPLICATE KEY UPDATE"
            + " paid_revenue = VALUES(paid_revenue), paid_bill_count = VALUES(paid_bill_count),"
            + " discount_amount = VALUES(discount_amount), legacy_revenue = VALUES(legacy_revenue),"
            + " legacy_visit_count = VALUES(legacy_visit_count), new_customers = VALUES(new_customers),"
            + " new_customers_male = VALUES(new_customers_male), new_customers_female = VALUES(new_customers_female),"
            + " new_customers_other = VALUES(new_customers_other), booking_requests = VALUES(booking_requests),"
            + " bookings_scheduled = VALUES(bookings_scheduled), bookings_done = VALUES(bookings_done),"
            + " bookings_not_arrived = VALUES(bookings_not_arrived), bookings_cancelled = VALUES(bookings_cancelled),"
            + " tickets_completed = VALUES(tickets_completed), computed_at = VALUES(computed_at)";

    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbc;

    public List<DailyRow> findBetween(LocalDate from, LocalDate to) {
        return jdbc.query("SELECT " + COLUMNS + " FROM dashboard_daily_summary"
                        + " WHERE summary_date BETWEEN ? AND ? ORDER BY summary_date",
                (rs, i) -> map(rs), Date.valueOf(from), Date.valueOf(to));
    }

    public void upsertAll(Collection<DailyRow> rows) {
        List<DailyRow> list = new ArrayList<>(rows);
        for (int i = 0; i < list.size(); i += BATCH_SIZE) {
            List<Object[]> batch = list.subList(i, Math.min(i + BATCH_SIZE, list.size())).stream()
                    .map(r -> new Object[]{
                            Date.valueOf(r.getDate()), r.getPaidRevenue(), r.getPaidBillCount(), r.getDiscountAmount(),
                            r.getLegacyRevenue(), r.getLegacyVisitCount(), r.getNewCustomers(),
                            r.getNewCustomersMale(), r.getNewCustomersFemale(), r.getNewCustomersOther(),
                            r.getBookingRequests(), r.getBookingsScheduled(), r.getBookingsDone(),
                            r.getBookingsNotArrived(), r.getBookingsCancelled(), r.getTicketsCompleted(),
                            Timestamp.valueOf(r.getComputedAt())})
                    .toList();
            jdbc.batchUpdate(UPSERT, batch);
        }
    }

    private static DailyRow map(ResultSet rs) throws SQLException {
        DailyRow r = new DailyRow(rs.getDate("summary_date").toLocalDate());
        r.setPaidRevenue(rs.getBigDecimal("paid_revenue"));
        r.setPaidBillCount(rs.getInt("paid_bill_count"));
        r.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        r.setLegacyRevenue(rs.getBigDecimal("legacy_revenue"));
        r.setLegacyVisitCount(rs.getInt("legacy_visit_count"));
        r.setNewCustomers(rs.getInt("new_customers"));
        r.setNewCustomersMale(rs.getInt("new_customers_male"));
        r.setNewCustomersFemale(rs.getInt("new_customers_female"));
        r.setNewCustomersOther(rs.getInt("new_customers_other"));
        r.setBookingRequests(rs.getInt("booking_requests"));
        r.setBookingsScheduled(rs.getInt("bookings_scheduled"));
        r.setBookingsDone(rs.getInt("bookings_done"));
        r.setBookingsNotArrived(rs.getInt("bookings_not_arrived"));
        r.setBookingsCancelled(rs.getInt("bookings_cancelled"));
        r.setTicketsCompleted(rs.getInt("tickets_completed"));
        Timestamp computedAt = rs.getTimestamp("computed_at");
        r.setComputedAt(computedAt != null ? computedAt.toLocalDateTime() : LocalDateTime.MIN);
        return r;
    }

    /** Một dòng của bảng cache = số liệu của một ngày. */
    @Data
    public static class DailyRow {
        private final LocalDate date;
        private BigDecimal paidRevenue = BigDecimal.ZERO;
        private int paidBillCount;
        private BigDecimal discountAmount = BigDecimal.ZERO;
        private BigDecimal legacyRevenue = BigDecimal.ZERO;
        private int legacyVisitCount;
        private int newCustomers;
        private int newCustomersMale;
        private int newCustomersFemale;
        private int newCustomersOther;
        private int bookingRequests;
        private int bookingsScheduled;
        private int bookingsDone;
        private int bookingsNotArrived;
        private int bookingsCancelled;
        private int ticketsCompleted;
        private LocalDateTime computedAt;
    }
}
