package com.g42.platform.gms.report.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Báo cáo khách hàng theo khoảng ngày: tổng số khách làm dịch vụ, tổng tiền thu được,
 * kèm phân rã theo từng khách hàng và từng phiếu dịch vụ (để đối chiếu / xuất Excel).
 *
 * Gộp hai nguồn: phiếu dịch vụ trong phần mềm và lượt nhập từ sổ Excel cũ
 * ({@code legacy_visit}). Mỗi dòng đều mang {@code source} để phân biệt, và phần tiền
 * của sổ cũ luôn được tách riêng ở {@code Summary.legacyRevenue} vì nó là con số chép
 * tay chứ không phải hoá đơn hệ thống.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerReportResponse {

    private Summary summary;
    private List<CustomerRow> customers;
    private List<TicketRow> tickets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private LocalDate from;
        private LocalDate to;
        private long totalCustomers;
        private long totalTickets;
        private long paidTickets;
        private long unpaidTickets;
        private BigDecimal totalRevenue;
        private BigDecimal revenueFromBills;
        private BigDecimal discountTotal;
        private BigDecimal averagePerCustomer;
        /** "dashboard" nếu tổng thu lấy từ bảng doanh thu tổng hợp, "bills" nếu cộng từ hoá đơn. */
        private String revenueSource;

        /** false khi người dùng bỏ tick "Gồm sổ cũ" — khi đó mọi số legacy* đều bằng 0. */
        private boolean legacyIncluded;
        /** Số lượt trong kỳ đến từ sổ Excel cũ (đã nằm trong totalTickets). */
        private long legacyVisits;
        /** Số khách trong kỳ chỉ có lượt sổ cũ, không có phiếu nào trong phần mềm. */
        private long legacyOnlyCustomers;
        /** Tiền ghi trong sổ cũ (đã nằm trong totalRevenue). Tách ra để đối chiếu sổ sách. */
        private BigDecimal legacyRevenue;
        /** Phần tổng thu đến từ phiếu trong phần mềm — totalRevenue trừ legacyRevenue. */
        private BigDecimal systemRevenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerRow {
        private Integer customerId;
        private String customerName;
        private String customerPhone;
        private List<String> plates;
        private long ticketCount;
        private long paidTicketCount;
        private BigDecimal revenue;
        private BigDecimal discountAmount;
        private List<String> ticketCodes;
        /** Phần của ticketCount đến từ sổ cũ. */
        private long legacyVisitCount;
        /** Phần của revenue đến từ sổ cũ. */
        private BigDecimal legacyRevenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketRow {
        private Integer serviceTicketId;
        private String ticketCode;
        private LocalDate date;
        private String customerName;
        private String customerPhone;
        private String licensePlate;
        private String ticketStatus;
        private String ticketType;
        private boolean paid;
        private boolean hasBill;
        private BigDecimal revenue;
        private BigDecimal discountAmount;
        /** "SYSTEM" = phiếu dịch vụ trong phần mềm, "LEGACY" = lượt nhập từ sổ Excel cũ. */
        private String source;
        /** Chỉ dòng LEGACY: id lượt sổ cũ, để mở đúng lượt khi cần rà lại. */
        private Integer legacyVisitId;
        /** Chỉ dòng LEGACY: chuỗi dịch vụ chép từ sổ, vì lượt cũ không có báo giá. */
        private String servicesText;
        /** Chỉ dòng LEGACY: tổng tiền trong sổ lệch tổng các dòng dịch vụ — cần rà lại. */
        private boolean amountMismatch;
    }
}
