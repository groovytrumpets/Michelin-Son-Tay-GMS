package com.g42.platform.gms.estimation.infrastructure.entity;

import com.g42.platform.gms.estimation.domain.enums.CommissionPartyType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Một khoản hoa hồng được phân bổ cho một bên trên phiếu báo giá.
 * Để trống estimateItemId nghĩa là hoa hồng tính trên toàn phiếu.
 */
@Entity
@Table(name = "commission_allocation")
@Getter
@Setter
public class CommissionAllocationJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "estimate_id", nullable = false)
    private Integer estimateId;

    @Column(name = "estimate_item_id")
    private Integer estimateItemId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "party_type", nullable = false, length = 20)
    private CommissionPartyType partyType;

    @Column(name = "partner_id")
    private Integer partnerId;

    @Column(name = "staff_id")
    private Integer staffId;

    @Column(name = "base_amount", precision = 12, scale = 2)
    private BigDecimal baseAmount;

    @Column(name = "rate_percent", precision = 5, scale = 2)
    private BigDecimal ratePercent;

    @ColumnDefault("0")
    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount;

    @Size(max = 255)
    @Column(name = "note")
    private String note;

    @Column(name = "created_by")
    private Integer createdBy;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;
}
