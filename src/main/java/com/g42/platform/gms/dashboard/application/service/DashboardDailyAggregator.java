package com.g42.platform.gms.dashboard.application.service;

import com.g42.platform.gms.billing.domain.enums.PaymentStatus;
import com.g42.platform.gms.billing.infrastructure.entity.ServiceBillJpa;
import com.g42.platform.gms.billing.infrastructure.repository.ServiceBillJpaRepo;
import com.g42.platform.gms.dashboard.infrastructure.DashboardDailySummaryStore.DailyRow;
import com.g42.platform.gms.report.application.service.RevenueReportService;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.ServiceTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Tính số liệu từng ngày từ bảng gốc để ghi vào cache {@code dashboard_daily_summary}.
 * Một lần gọi quét cả khoảng ngày bằng vài truy vấn GROUP BY — không lặp từng ngày.
 *
 * <p>Doanh thu đi qua JPA (không GROUP BY DATE(paid_at) bằng SQL) vì {@code paid_at} là
 * {@code Instant}: ngày thu phải quy theo múi giờ máy chủ đúng như trang Quản lý doanh thu,
 * và dùng chung quy tắc lọc phiếu / chọn hoá đơn với {@link RevenueReportService}. Các cột
 * còn lại là DATETIME / DATE thường nên gom thẳng bằng SQL.
 */
@Component
@RequiredArgsConstructor
public class DashboardDailyAggregator {

    private static final String WALK_IN_CUSTOMER_CODE = "KHACH_LE";

    private final JdbcTemplate jdbc;
    private final ServiceBillJpaRepo serviceBillJpaRepo;
    private final ServiceTicketRepository serviceTicketRepository;

    /** Mọi ngày trong [from, to] đều có dòng (ngày không có gì = dòng toàn số 0). */
    @Transactional(readOnly = true)
    public Map<LocalDate, DailyRow> compute(LocalDate from, LocalDate to) {
        Map<LocalDate, DailyRow> rows = new TreeMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            rows.put(d, new DailyRow(d));
        }
        Timestamp start = Timestamp.valueOf(from.atStartOfDay());
        Timestamp endExclusive = Timestamp.valueOf(to.plusDays(1).atStartOfDay());

        addPaidBills(rows, from, to);
        addLegacyVisits(rows, start, endExclusive);
        addNewCustomers(rows, start, endExclusive);
        addBookingRequests(rows, start, endExclusive);
        addBookings(rows, from, to);
        addCompletedTickets(rows, start, endExclusive);

        LocalDateTime now = LocalDateTime.now();
        rows.values().forEach(r -> r.setComputedAt(now));
        return rows;
    }

    /** Hoá đơn đã thu theo ngày thu; mỗi phiếu chỉ tính một hoá đơn, bỏ phiếu xoá / huỷ / nhập bù bị từ chối. */
    private void addPaidBills(Map<LocalDate, DailyRow> rows, LocalDate from, LocalDate to) {
        ZoneId zone = ZoneId.systemDefault();
        List<ServiceBillJpa> bills = serviceBillJpaRepo.findByPaidAtBetween(
                        from.atStartOfDay(zone).toInstant(),
                        to.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1_000)).stream()
                .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID && b.getPaidAt() != null)
                .toList();
        if (bills.isEmpty()) {
            return;
        }

        Set<Integer> ticketIds = bills.stream().map(ServiceBillJpa::getServiceTicketId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Integer, ServiceTicketJpa> tickets = new HashMap<>();
        serviceTicketRepository.findAllById(ticketIds).forEach(t -> tickets.put(t.getServiceTicketId(), t));

        Map<Integer, ServiceBillJpa> billByTicket = new HashMap<>();
        for (ServiceBillJpa bill : bills) {
            ServiceTicketJpa ticket = tickets.get(bill.getServiceTicketId());
            if (ticket == null || !RevenueReportService.isCountable(ticket)) {
                continue;
            }
            billByTicket.merge(ticket.getServiceTicketId(), bill,
                    (a, b) -> RevenueReportService.preferBill(b, a) ? b : a);
        }

        for (ServiceBillJpa bill : billByTicket.values()) {
            DailyRow row = rows.get(LocalDateTime.ofInstant(bill.getPaidAt(), zone).toLocalDate());
            if (row == null) {
                continue;
            }
            row.setPaidRevenue(row.getPaidRevenue().add(nvl(bill.getFinalAmount())));
            row.setDiscountAmount(row.getDiscountAmount().add(nvl(bill.getDiscountAmount())));
            row.setPaidBillCount(row.getPaidBillCount() + 1);
        }
    }

    /** Sổ dịch vụ cũ: chỉ lượt có tiền, giống trang Quản lý doanh thu. */
    private void addLegacyVisits(Map<LocalDate, DailyRow> rows, Timestamp start, Timestamp endExclusive) {
        jdbc.query("""
                        SELECT DATE(visited_at) AS d, COUNT(*) AS c, COALESCE(SUM(total_amount), 0) AS s
                        FROM legacy_visit
                        WHERE visited_at >= ? AND visited_at < ? AND total_amount > 0
                        GROUP BY DATE(visited_at)
                        """,
                rs -> {
                    DailyRow row = rows.get(rs.getDate("d").toLocalDate());
                    if (row != null) {
                        row.setLegacyVisitCount(rs.getInt("c"));
                        row.setLegacyRevenue(nvl(rs.getBigDecimal("s")));
                    }
                }, start, endExclusive);
    }

    /** Hồ sơ khách tạo trong ngày, trừ hồ sơ dùng chung "Khách lẻ". */
    private void addNewCustomers(Map<LocalDate, DailyRow> rows, Timestamp start, Timestamp endExclusive) {
        jdbc.query("""
                        SELECT DATE(created_at) AS d, gender AS g, COUNT(*) AS c
                        FROM customer_profile
                        WHERE created_at >= ? AND created_at < ?
                          AND (customer_code IS NULL OR customer_code <> ?)
                        GROUP BY DATE(created_at), gender
                        """,
                rs -> {
                    DailyRow row = rows.get(rs.getDate("d").toLocalDate());
                    if (row == null) {
                        return;
                    }
                    int count = rs.getInt("c");
                    row.setNewCustomers(row.getNewCustomers() + count);
                    String gender = rs.getString("g");
                    if ("MALE".equals(gender)) {
                        row.setNewCustomersMale(row.getNewCustomersMale() + count);
                    } else if ("FEMALE".equals(gender)) {
                        row.setNewCustomersFemale(row.getNewCustomersFemale() + count);
                    } else if ("OTHER".equals(gender)) {
                        row.setNewCustomersOther(row.getNewCustomersOther() + count);
                    }
                }, start, endExclusive, WALK_IN_CUSTOMER_CODE);
    }

    private void addBookingRequests(Map<LocalDate, DailyRow> rows, Timestamp start, Timestamp endExclusive) {
        jdbc.query("""
                        SELECT DATE(created_at) AS d, COUNT(*) AS c
                        FROM booking_request
                        WHERE created_at >= ? AND created_at < ? AND status <> 'SPAM'
                        GROUP BY DATE(created_at)
                        """,
                rs -> {
                    DailyRow row = rows.get(rs.getDate("d").toLocalDate());
                    if (row != null) {
                        row.setBookingRequests(rs.getInt("c"));
                    }
                }, start, endExclusive);
    }

    /**
     * Lịch hẹn theo ngày hẹn và trạng thái. Bỏ DRAFT (chưa thành lịch) và lịch hẹn lấy hàng
     * của đơn bán lẻ (is_parts_sale) — tỷ lệ "đặt lịch thành công" chỉ nói về lịch sửa xe.
     */
    private void addBookings(Map<LocalDate, DailyRow> rows, LocalDate from, LocalDate to) {
        jdbc.query("""
                        SELECT scheduled_date AS d, status AS st, COUNT(*) AS c
                        FROM booking
                        WHERE scheduled_date BETWEEN ? AND ?
                          AND status <> 'DRAFT'
                          AND COALESCE(is_parts_sale, 0) = 0
                        GROUP BY scheduled_date, status
                        """,
                rs -> {
                    DailyRow row = rows.get(rs.getDate("d").toLocalDate());
                    if (row == null) {
                        return;
                    }
                    int count = rs.getInt("c");
                    row.setBookingsScheduled(row.getBookingsScheduled() + count);
                    switch (String.valueOf(rs.getString("st"))) {
                        case "DONE" -> row.setBookingsDone(row.getBookingsDone() + count);
                        case "NOT_ARRIVED" -> row.setBookingsNotArrived(row.getBookingsNotArrived() + count);
                        case "CANCELLED" -> row.setBookingsCancelled(row.getBookingsCancelled() + count);
                        default -> {
                        }
                    }
                }, Date.valueOf(from), Date.valueOf(to));
    }

    /**
     * Phiếu sửa xe KTV báo xong trong ngày. Tính cả phiếu đã chuyển sang PAID — completed_at
     * được ghi lúc báo xong và giữ nguyên khi thu tiền.
     */
    private void addCompletedTickets(Map<LocalDate, DailyRow> rows, Timestamp start, Timestamp endExclusive) {
        jdbc.query("""
                        SELECT DATE(completed_at) AS d, COUNT(*) AS c
                        FROM service_ticket
                        WHERE completed_at >= ? AND completed_at < ?
                          AND ticket_status IN ('COMPLETED', 'PAID')
                          AND COALESCE(is_deleted, 0) = 0
                          AND (ticket_type IS NULL OR ticket_type <> 'PARTS_SALE')
                          AND (backfill_review_status IS NULL OR backfill_review_status <> 'REJECTED')
                        GROUP BY DATE(completed_at)
                        """,
                rs -> {
                    DailyRow row = rows.get(rs.getDate("d").toLocalDate());
                    if (row != null) {
                        row.setTicketsCompleted(rs.getInt("c"));
                    }
                }, start, endExclusive);
    }

    private static BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
