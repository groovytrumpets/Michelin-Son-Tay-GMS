package com.g42.platform.gms.warehouse.infrastructure.entity;

import com.g42.platform.gms.warehouse.domain.enums.SerialStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** Một đơn vị hàng có số serial; luôn thuộc một lô nhập (stock_entry_item). */
@Entity
@Table(name = "item_serial")
@Data
public class ItemSerialJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "serial_id")
    private Integer serialId;

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(name = "warehouse_id", nullable = false)
    private Integer warehouseId;

    @Column(name = "entry_item_id", nullable = false)
    private Integer entryItemId;

    @Column(name = "serial_code", nullable = false, length = 100)
    private String serialCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SerialStatus status = SerialStatus.IN_STOCK;

    /** Dòng báo giá đang giữ serial (RESERVED). */
    @Column(name = "estimate_item_id")
    private Integer estimateItemId;

    /** Phiếu xuất đã bán serial (SOLD). */
    @Column(name = "issue_id")
    private Integer issueId;

    @Column(name = "attributes_json", columnDefinition = "TEXT")
    private String attributesJson;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
