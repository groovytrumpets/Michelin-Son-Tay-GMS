package com.g42.platform.gms.dashboard.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Số liệu cho các widget của trang /dashboard trong kỳ [from, to].
 *
 * <p>Mỗi nhóm là {@code null} khi người xem không có quyền tương ứng (revenue → REVENUE_VIEW,
 * bookings → BOOKING_VIEW, customers → CUSTOMER_VIEW); các trường cùng nhóm trong
 * {@link TrendPoint} cũng để {@code null}. Nhóm {@code personal} là của chính người đang
 * đăng nhập nên luôn có.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private LocalDate from;
    private LocalDate to;
    /** Chuỗi {@link #trend} bắt đầu từ ngày này — ít nhất 7 ngày để kỳ "Hôm nay" vẫn có đường xu hướng. */
    private LocalDate trendFrom;
    /** DAY / MONTH / YEAR — mỗi điểm trong {@link #trend} là một ngày, một tháng hay một năm. */
    private String trendGranularity;
    /** Kỳ "Tất cả": from là ngày sớm nhất có dữ liệu. */
    private boolean allTime;
    private LocalDateTime generatedAt;

    private Revenue revenue;
    private Bookings bookings;
    private Customers customers;
    private Tickets tickets;
    private Personal personal;

    private List<TrendPoint> trend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Revenue {
        /** Tổng thực thu của hoá đơn trong hệ thống (không gồm sổ cũ). */
        private BigDecimal paidRevenue;
        /** Ước tính trước thuế: paidRevenue / 1,1 — hoá đơn chưa lưu tiền thuế riêng. */
        private BigDecimal revenueNoTax;
        private BigDecimal discountAmount;
        private long paidBillCount;
        /** Tiền sổ dịch vụ cũ trong kỳ — để riêng, không cộng vào paidRevenue. */
        private BigDecimal legacyRevenue;
        private long legacyVisitCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Bookings {
        /** Yêu cầu đặt lịch đang chờ duyệt có ngày hẹn tới hết ngày {@code to} (số hiện tại, không cache). */
        private long pendingDue;
        /** Yêu cầu đang chờ duyệt được gửi trong kỳ (kỳ "Tất cả" = mọi yêu cầu đang chờ). */
        private long pendingInRange;
        /** Yêu cầu đặt lịch gửi trong kỳ. */
        private long requests;
        private long scheduled;
        private long done;
        private long notArrived;
        private long cancelled;
        /** done / (done + notArrived + cancelled) × 100; null khi chưa lịch nào kết thúc. */
        private Double successRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Customers {
        /** Hồ sơ khách tạo trong kỳ (kỳ "Tất cả" = mọi hồ sơ). */
        private long newCustomers;
        private long male;
        private long female;
        private long other;
        /** Chưa khai giới tính. */
        private long unknown;
        /** Tổng hồ sơ khách hiện có, không phụ thuộc kỳ. */
        private long totalCustomers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Tickets {
        /** Phiếu sửa xe báo xong trong kỳ (toàn xưởng). */
        private long completed;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Personal {
        /** Giờ làm: giờ vào → giờ ra thật; thiếu giờ ra thì lấy độ dài ca của ngày có mặt. */
        private double workHours;
        /** Ngày có mặt (đúng giờ / muộn / về sớm). */
        private long presentDays;
        /** Ngày có xếp ca tính tới hôm nay (trừ ngày nghỉ). */
        private long scheduledDays;
        private long onTimeDays;
        private long lateDays;
        private long earlyLeaveDays;
        private long absentDays;
        /** Phiếu người này được phân công và đã báo xong trong kỳ. */
        private long completedTickets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPoint {
        private LocalDate date;
        private BigDecimal paidRevenue;
        private BigDecimal legacyRevenue;
        private Integer paidBillCount;
        private Integer newCustomers;
        private Integer bookingRequests;
        private Integer bookingsScheduled;
        private Integer bookingsDone;
        private Integer bookingsNotArrived;
        private Integer bookingsCancelled;
        private Integer ticketsCompleted;
        private Double workHours;
        private Integer presentDays;
        private Integer personalTicketsCompleted;
    }
}
