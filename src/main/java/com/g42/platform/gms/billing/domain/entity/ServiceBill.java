package com.g42.platform.gms.billing.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBill {

    private Integer billId;
    private Integer serviceTicketId;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private Instant paidAt;
    private String billStatus;
    private Integer warehouseId;
    private Integer version;
    private Integer estimateId;
    private Integer promotionId;
}
