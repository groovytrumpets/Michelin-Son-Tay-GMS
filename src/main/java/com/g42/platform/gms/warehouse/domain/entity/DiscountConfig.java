package com.g42.platform.gms.warehouse.domain.entity;

import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DiscountConfig {
    private Integer configId;
    private Integer itemId;       // null = áp dụng tất cả
    private IssueType issueType;  // null = không lọc theo loại xuất
    private Integer quantityThreshold; // null = không theo ngưỡng
    private BigDecimal discountRate;
    private Boolean isActive;
    private Integer createdBy;
    private LocalDateTime createdAt;
}
