package com.g42.platform.gms.warehouse.api.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CommissionReportResponse {
    private Integer recordId;
    private Integer staffId;
    private Integer itemId;
    private Integer issueId;
    private Integer quantity;
    private BigDecimal finalPrice;
    private BigDecimal commissionRate;
    private BigDecimal commissionValue;
    private String periodMonth;
    private LocalDateTime createdAt;
}
