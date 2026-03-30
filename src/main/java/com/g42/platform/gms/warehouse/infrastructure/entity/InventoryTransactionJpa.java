package com.g42.platform.gms.warehouse.infrastructure.entity;


import com.g42.platform.gms.warehouse.domain.enums.InventoryTransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "inventory_transaction", schema = "michelin_garage")
public class InventoryTransactionJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id", nullable = false)
    private Integer transactionId;

    @NotNull
    @Column(name = "warehouse_id", nullable = false)
    private Integer warehouseId;

    @NotNull
    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @NotNull
    @Lob
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private InventoryTransactionType transactionType;

    @NotNull
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @Size(max = 50)
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Integer referenceId;

    @Lob
    @Column(name = "notes")
    private String notes;

    @NotNull
    @Column(name = "created_by", nullable = false)
    private Integer createdById;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;


}