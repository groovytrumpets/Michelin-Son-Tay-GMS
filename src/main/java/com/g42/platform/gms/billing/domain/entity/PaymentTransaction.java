package com.g42.platform.gms.billing.domain.entity;

import com.g42.platform.gms.billing.domain.enums.PaymentTransactionStatus;
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
public class PaymentTransaction {
    private Integer transactionId;
    private Integer billId;
    private BigDecimal amount;
    private String method;
    private PaymentTransactionStatus status;
    private Instant paidAt;
}
