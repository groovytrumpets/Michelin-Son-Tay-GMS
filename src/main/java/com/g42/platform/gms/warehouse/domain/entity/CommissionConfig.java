package com.g42.platform.gms.warehouse.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CommissionConfig {
    private Integer configId;
    private Integer itemId;
    private BigDecimal commissionRate;
    private Integer commissionQuantityThreshold;
    private Boolean isActive;
    private Integer createdBy;
    private LocalDateTime createdAt;
}
