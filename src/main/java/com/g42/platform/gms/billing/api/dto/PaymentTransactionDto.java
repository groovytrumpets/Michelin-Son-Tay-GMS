package com.g42.platform.gms.billing.api.dto;

import com.g42.platform.gms.billing.infrastructure.entity.ServiceBillJpa;
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
public class PaymentTransactionDto {
    private Integer billId;
    private BigDecimal amount;
    private String method;
}
