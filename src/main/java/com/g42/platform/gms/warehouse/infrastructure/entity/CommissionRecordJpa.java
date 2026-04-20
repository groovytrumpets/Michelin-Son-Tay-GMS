package com.g42.platform.gms.warehouse.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commission_record")
@Data
public class CommissionRecordJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Integer recordId;

    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(name = "issue_id", nullable = false)
    private Integer issueId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "final_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal finalPrice;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(name = "commission_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionValue;

    @Column(name = "period_month", length = 7, nullable = false)
    private String periodMonth; // YYYY-MM

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
