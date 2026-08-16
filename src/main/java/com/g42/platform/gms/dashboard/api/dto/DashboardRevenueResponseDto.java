package com.g42.platform.gms.dashboard.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardRevenueResponseDto {

    private KpisDto kpis;
    private List<TrendPointDto> trend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpisDto {
        private BigDecimal totalRevenue;
        private BigDecimal paidRevenue;
        private BigDecimal unpaidRevenue;
        private BigDecimal overdueRevenue;
        private BigDecimal revenueNoTax;
        private BigDecimal discountAmount;
        private Long invoiceCount;
        private BigDecimal averageTicketValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendPointDto {
        private String date;
        private BigDecimal revenue;
        private BigDecimal paid;
        private BigDecimal unpaid;
    }
}
