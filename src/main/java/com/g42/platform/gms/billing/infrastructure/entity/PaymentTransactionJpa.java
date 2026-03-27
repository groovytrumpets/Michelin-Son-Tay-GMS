package com.g42.platform.gms.billing.infrastructure.entity;

import com.g42.platform.gms.billing.domain.enums.PaymentTransactionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "payment_transaction", schema = "michelin_garage")
public class PaymentTransactionJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id", nullable = false)
    private Integer transactionId;

    @NotNull
    @Column(name = "bill_id", nullable = false)
    private Integer billId;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Size(max = 50)
    @Column(name = "method", length = 50)
    private String method;
    @Enumerated(EnumType.STRING)
    @Lob
    @Column(name = "status")
    private PaymentTransactionStatus status;

    @Column(name = "paid_at")
    private Instant paidAt;


}