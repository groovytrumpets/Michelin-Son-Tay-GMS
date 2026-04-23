package com.g42.platform.gms.warehouse.infrastructure.entity;

import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity(name = "WarehouseStockAllocation")
@Table(name = "stock_allocation",
        indexes = {
                @Index(name = "idx_alloc_ticket", columnList = "service_ticket_id"),
        @Index(name = "idx_alloc_issue", columnList = "issue_id"),
                @Index(name = "idx_alloc_item", columnList = "warehouse_id, item_id")
        })
@Data
public class StockAllocationJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_id")
    private Integer allocationId;

    @Column(name = "service_ticket_id", nullable = false)
    private Integer serviceTicketId;

    @Column(name = "issue_id")
    private Integer issueId;

    @Column(name = "estimate_item_id", nullable = false)
    private Integer estimateItemId;

    @Column(name = "warehouse_id", nullable = false)
    private Integer warehouseId;

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AllocationStatus status = AllocationStatus.RESERVED;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
