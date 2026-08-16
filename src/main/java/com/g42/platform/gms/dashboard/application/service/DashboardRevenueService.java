package com.g42.platform.gms.dashboard.application.service;

import com.g42.platform.gms.dashboard.api.dto.DashboardRevenueResponseDto;
import com.g42.platform.gms.dashboard.api.dto.DashboardRevenueResponseDto.KpisDto;
import com.g42.platform.gms.dashboard.api.dto.DashboardRevenueResponseDto.TrendPointDto;
import com.g42.platform.gms.dashboard.infrastructure.entity.DashboardRevenueSummaryJpa;
import com.g42.platform.gms.dashboard.infrastructure.repository.DashboardRevenueSummaryJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardRevenueService {

    private final DashboardRevenueSummaryJpaRepo revenueSummaryRepo;

    /**
     * Lay bao cao doanh thu tong hop tu bang Summary Table (Materialized View pattern) theo khoang ngay.
     */
    public DashboardRevenueResponseDto getRevenueReport(LocalDate fromDate, LocalDate toDate) {
        LocalDate from = fromDate != null ? fromDate : LocalDate.now().withDayOfMonth(1);
        LocalDate to = toDate != null ? toDate : LocalDate.now();

        List<DashboardRevenueSummaryJpa> summaries = revenueSummaryRepo.findByReportDateBetweenOrderByReportDateAsc(from, to);

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal paidRevenue = BigDecimal.ZERO;
        BigDecimal unpaidRevenue = BigDecimal.ZERO;
        BigDecimal overdueRevenue = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;
        long totalInvoices = 0;

        List<TrendPointDto> trendPoints = new ArrayList<>();

        for (DashboardRevenueSummaryJpa s : summaries) {
            BigDecimal dayTotal = s.getTotalRevenue() != null ? s.getTotalRevenue() : BigDecimal.ZERO;
            BigDecimal dayPaid = s.getPaidRevenue() != null ? s.getPaidRevenue() : BigDecimal.ZERO;
            BigDecimal dayUnpaid = s.getUnpaidRevenue() != null ? s.getUnpaidRevenue() : BigDecimal.ZERO;
            BigDecimal dayOverdue = s.getOverdueRevenue() != null ? s.getOverdueRevenue() : BigDecimal.ZERO;
            BigDecimal dayDiscount = s.getDiscountAmount() != null ? s.getDiscountAmount() : BigDecimal.ZERO;
            int dayInvoices = s.getInvoiceCount() != null ? s.getInvoiceCount() : 0;

            totalRevenue = totalRevenue.add(dayTotal);
            paidRevenue = paidRevenue.add(dayPaid);
            unpaidRevenue = unpaidRevenue.add(dayUnpaid);
            overdueRevenue = overdueRevenue.add(dayOverdue);
            discountAmount = discountAmount.add(dayDiscount);
            totalInvoices += dayInvoices;

            trendPoints.add(TrendPointDto.builder()
                    .date(s.getReportDate().toString())
                    .revenue(dayTotal)
                    .paid(dayPaid)
                    .unpaid(dayUnpaid)
                    .build());
        }

        // Compute revenue without tax (paidRevenue / 1.1)
        BigDecimal revenueNoTax = paidRevenue.divide(new BigDecimal("1.1"), 0, RoundingMode.HALF_UP);
        BigDecimal averageTicketValue = totalInvoices > 0 
                ? paidRevenue.divide(BigDecimal.valueOf(totalInvoices), 0, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO;

        KpisDto kpis = KpisDto.builder()
                .totalRevenue(totalRevenue)
                .paidRevenue(paidRevenue)
                .unpaidRevenue(unpaidRevenue)
                .overdueRevenue(overdueRevenue)
                .revenueNoTax(revenueNoTax)
                .discountAmount(discountAmount)
                .invoiceCount(totalInvoices)
                .averageTicketValue(averageTicketValue)
                .build();

        return DashboardRevenueResponseDto.builder()
                .kpis(kpis)
                .trend(trendPoints)
                .build();
    }
}
