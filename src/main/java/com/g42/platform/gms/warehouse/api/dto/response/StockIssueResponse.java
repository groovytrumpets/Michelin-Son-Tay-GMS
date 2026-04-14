package com.g42.platform.gms.warehouse.api.dto.response;

import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StockIssueResponse {
    private Integer issueId;
    private String issueCode;
    private Integer warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private IssueType issueType;
    private String issueReason;
    private Integer serviceTicketId;
    private String serviceTicketCode;
    private BigDecimal discountRate;
    private StockIssueStatus status;
    private Integer confirmedBy;
    private String confirmedByName;
    private LocalDateTime confirmedAt;
    private Integer createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}
