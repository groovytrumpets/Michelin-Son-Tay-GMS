package com.g42.platform.gms.warehouse.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Gộp stock_entry_attachment + return_entry_attachment thành 1 bảng.
 * ref_type phân biệt loại chứng từ, ref_id là PK của bảng tương ứng.
 */
@Entity
@Table(name = "warehouse_attachment",
        indexes = {
                @Index(name = "idx_wa_ref", columnList = "ref_type, ref_id")
        })
@Data
public class WarehouseAttachmentJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Integer attachmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false)
    private RefType refType;

    @Column(name = "ref_id", nullable = false)
    private Integer refId;

    @Column(name = "file_url", length = 500, nullable = false)
    private String fileUrl;

    @Column(name = "uploaded_by", nullable = false)
    private Integer uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    public enum RefType {
        STOCK_ENTRY,
        STOCK_ISSUE,
        RETURN_ENTRY_ITEM
    }
}
