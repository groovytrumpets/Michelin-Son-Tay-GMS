package com.g42.platform.gms.estimation.infrastructure.entity;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
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
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_ticket_id", nullable = false)
    private ServiceTicketJpa serviceTicket;

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
    @Column(name = "payment_status", nullable = false)
    private String paymentStatus;

    @Column(name = "paid_at")
    private Instant paidAt;

    @ColumnDefault("'DRAFT'")
    @Lob
    @Column(name = "bill_status")
    private String billStatus;

    @Column(name = "warehouse_id")
    private Integer warehouseId;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "version", nullable = false)
    private Integer version;

    @NotNull
    @JoinColumn(name = "estimate_estimate_id", nullable = false)
    private Integer estimateId;


}