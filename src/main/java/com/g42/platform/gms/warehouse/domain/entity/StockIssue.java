package com.g42.platform.gms.warehouse.domain.entity;

import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockIssue {
    private Integer issueId;
    private String issueCode;
    private Integer warehouseId;
    private IssueType issueType;
    private String issueReason;
    private Integer serviceTicketId;
    private BigDecimal discountRate;
    private StockIssueStatus status;
    private Integer confirmedBy;
    private LocalDateTime confirmedAt;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private List<StockIssueItem> items;
}
