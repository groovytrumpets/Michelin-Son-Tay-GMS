package com.g42.platform.gms.warehouse.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CommissionRecord {
    private Integer recordId;
    private Integer staffId;
    private Integer itemId;
    private Integer issueId;
    private Integer quantity;
    private BigDecimal finalPrice;
    private BigDecimal commissionRate;
    private BigDecimal commissionValue; // finalPrice * quantity * rate / 100
    private String periodMonth;         // YYYY-MM
    private LocalDateTime createdAt;
}
