package com.g42.platform.gms.billing.api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBillDto {
    private Integer billId;
    private Integer serviceTicketId;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String paymentStatus;
    private Instant paidAt;
    private Integer warehouseId;
    private Integer estimateId;
    private Integer promotionId;
}
