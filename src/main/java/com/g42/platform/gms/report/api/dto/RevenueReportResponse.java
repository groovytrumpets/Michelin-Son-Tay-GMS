package com.g42.platform.gms.report.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Báo cáo doanh thu theo khoảng ngày cho trang /revenue-management.
 *
 * Mỗi giao dịch là một hoá đơn ({@code service_bill}) của một phiếu. Hoá đơn đã thu tính theo
 * ngày thu tiền ({@code paid_at}), hoá đơn chưa thu tính theo ngày tiếp nhận phiếu. Các biểu đồ
 * (xu hướng, hạng mục, nhân viên...) do frontend tự gom từ {@code transactions} để lọc trạng
 * thái / loại / từ khoá ngay trên trình duyệt mà không phải gọi lại API.
 *
 * Tiền sổ dịch vụ cũ ({@code legacy_visit}) chỉ có mặt khi {@code includeLegacy}, luôn mang
 * {@code source = LEGACY} và được tách riêng ở {@code Summary.legacyRevenue}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueReportResponse {

    private Summary summary;
    private List<Transaction> transactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private LocalDate from;
        private LocalDate to;
        private BigDecimal subtotal;
        private BigDecimal discountAmount;
        private BigDecimal totalRevenue;
        private BigDecimal paidRevenue;
        private BigDecimal unpaidRevenue;
        private BigDecimal overdueRevenue;
        private long invoiceCount;
        private long paidCount;
        /** Hoá đơn chưa thu quá số ngày này (tính từ ngày tiếp nhận) thì coi là quá hạn. */
        private int overdueAfterDays;
        private boolean legacyIncluded;
        private long legacyCount;
        private BigDecimal legacyRevenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Transaction {
        /** Khoá duy nhất của dòng: {@code B-<billId>} hoặc {@code L-<legacyVisitId>}. */
        private String id;
        private Integer serviceTicketId;
        private Integer billId;
        private Integer legacyVisitId;
        private String ticketCode;
        private String customerName;
        private String customerPhone;
        private String licensePlate;
        /** Ngày ghi nhận doanh thu: ngày thu tiền nếu đã thu, ngược lại là ngày tiếp nhận. */
        private LocalDate date;
        private LocalDateTime receivedAt;
        private LocalDateTime paidAt;
        /** SERVICE (phiếu dịch vụ) | PART (phiếu bán phụ tùng) | LEGACY (sổ cũ). */
        private String type;
        private String ticketStatus;
        /** Hạng mục chiếm nhiều tiền nhất trong hoá đơn. */
        private String category;
        private String staffName;
        /** Xưởng làm phiếu; sổ cũ tính là của xưởng mặc định. */
        private String branchName;
        private BigDecimal subtotal;
        private BigDecimal discountAmount;
        private BigDecimal totalAmount;
        /** Mã phương thức thu tiền (CASH, TRANSFER...); rỗng nếu chưa thu. */
        private String paymentMethod;
        /** PAID | UNPAID | OVERDUE. */
        private String status;
        private String source;
        /** Phân rã tiền theo hạng mục; đã chia tỉ lệ để cộng lại đúng bằng {@code totalAmount}. */
        private List<Line> lines;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {
        private String category;
        /** SERVICE | PART | OTHER | LEGACY. */
        private String lineType;
        private BigDecimal amount;
    }
}
