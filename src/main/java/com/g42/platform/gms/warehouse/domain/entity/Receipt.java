package com.g42.platform.gms.warehouse.domain.entity;

import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.enums.PaymentStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Receipt {
    private Integer receiptId;
    private String receiptCode;
    private Integer issueId;
    private BigDecimal totalAmount;
    private IssueType issueType;
    private PaymentStatus paymentStatus;
    private Integer paidBy;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
