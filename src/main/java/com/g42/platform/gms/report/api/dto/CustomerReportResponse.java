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
    }
}
