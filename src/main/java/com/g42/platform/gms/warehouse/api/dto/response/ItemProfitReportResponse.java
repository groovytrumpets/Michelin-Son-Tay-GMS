package com.g42.platform.gms.warehouse.api.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItemProfitReportResponse {
    private Integer itemId;
    private String itemName;
    private String itemType;
    private Integer quantity;
    private BigDecimal revenue;
    private BigDecimal cost;
    private BigDecimal grossProfit;
    private BigDecimal marginPct;
}
