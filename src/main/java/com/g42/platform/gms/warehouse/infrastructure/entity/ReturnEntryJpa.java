package com.g42.platform.gms.warehouse.infrastructure.entity;

import com.g42.platform.gms.warehouse.domain.enums.ReturnEntryStatus;
import com.g42.platform.gms.warehouse.domain.enums.ReturnType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Phiếu hoàn hàng (header).
 * Một phiếu hoàn có thể gồm nhiều sản phẩm → return_entry_item.
 * Mỗi sản phẩm có ảnh lỗi riêng qua warehouse_attachment
 * (ref_type = RETURN_ENTRY_ITEM, ref_id = return_item_id).
 */
@Entity
@Table(name = "return_entry")
@Data
public class ReturnEntryJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "return_id")
    private Integer returnId;

    @Column(name = "return_code", length = 30, nullable = false, unique = true)
    private String returnCode;

    @Column(name = "warehouse_id", nullable = false)
    private Integer warehouseId;

    @Column(name = "return_reason", columnDefinition = "TEXT", nullable = false)
    private String returnReason;

    @Column(name = "source_issue_id")
    private Integer sourceIssueId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReturnEntryStatus status = ReturnEntryStatus.DRAFT;

    /**
     * Loại phiếu hoàn:
     * CUSTOMER_RETURN → cộng inventory (hàng về kho)
     * SUPPLIER_RETURN → trừ inventory (hàng rời kho về NCC)
     * EXCHANGE → cộng hàng lỗi + trừ hàng mới xuất
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "return_type", nullable = false)
    private ReturnType returnType = ReturnType.CUSTOMER_RETURN;

    @Column(name = "confirmed_by")
    private Integer confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "created_by", nullable = false)
    private Integer createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "returnId", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ReturnEntryItemJpa> items = new ArrayList<>();

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
