package com.g42.platform.gms.warehouse.app.service.report;

import com.g42.platform.gms.warehouse.api.dto.response.ItemProfitReportResponse;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.ItemProfitAggregate;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockIssueRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseReportService {

    private final StockIssueRepo stockIssueRepo;
    private final CatalogItemRepo catalogItemRepo;

    @Transactional(readOnly = true)
    public List<ItemProfitReportResponse> getProfitByItem(LocalDate fromDate, LocalDate toDate, Integer warehouseId) {
        List<ItemProfitAggregate> aggregates = stockIssueRepo.aggregateItemProfit(warehouseId, fromDate, toDate);

        return aggregates.stream().map(agg -> {
            ItemProfitReportResponse resp = new ItemProfitReportResponse();
            resp.setItemId(agg.getItemId());
            resp.setQuantity(agg.getTotalQuantity());
            resp.setRevenue(agg.getTotalRevenue());
            resp.setCost(agg.getTotalCost());
            resp.setGrossProfit(agg.getTotalGrossProfit());
            resp.setMarginPct(computeMarginPct(agg.getTotalGrossProfit(), agg.getTotalRevenue()));

            CatalogItem catalogItem = catalogItemRepo.getCatalogItemById(agg.getItemId());
            if (catalogItem != null) {
                resp.setItemName(catalogItem.getItemName());
                resp.setItemType(catalogItem.getItemType() != null ? catalogItem.getItemType().name() : null);
            }
            return resp;
        }).toList();
    }

    private BigDecimal computeMarginPct(BigDecimal grossProfit, BigDecimal revenue) {
        if (revenue == null || revenue.compareTo(BigDecimal.ZERO) == 0 || grossProfit == null) {
            return BigDecimal.ZERO;
        }
        return grossProfit
                .divide(revenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
