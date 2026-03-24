package com.g42.platform.gms.billing.infrastructure.entity;

import com.g42.platform.gms.billing.domain.enums.BillingStatus;
import com.g42.platform.gms.billing.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "service_bill", schema = "michelin_garage")
public class ServiceBillJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_id", nullable = false)
    private Integer billId;

    @NotNull
    @Column(name = "service_ticket_id", nullable = false)
    private Integer serviceTicketId;

    @Column(name = "sub_total", precision = 12, scale = 2)
    private BigDecimal subTotal;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "final_amount", precision = 12, scale = 2)
    private BigDecimal finalAmount;

    @Size(max = 50)
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @NotNull
    @ColumnDefault("'UNPAID'")
    @Lob
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "paid_at")
    private Instant paidAt;
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'DRAFT'")
    @Lob
    @Column(name = "bill_status")
    private BillingStatus billStatus;

    @Column(name = "warehouse_id")
    private Integer warehouseId;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "estimate_id", nullable = false)
    private Integer estimateId;
    @Column(name = "promotion_id", nullable = true)
    private Integer promotionId;


}