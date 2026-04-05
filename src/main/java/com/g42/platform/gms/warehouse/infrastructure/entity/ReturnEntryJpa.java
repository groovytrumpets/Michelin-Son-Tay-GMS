package com.g42.platform.gms.warehouse.infrastructure.entity;

import com.g42.platform.gms.warehouse.domain.enums.ReturnEntryStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "item_id", nullable = false)
    private Integer itemId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "return_reason", columnDefinition = "TEXT", nullable = false)
    private String returnReason;

    @Column(name = "condition_note", columnDefinition = "TEXT", nullable = false)
    private String conditionNote;

    @Column(name = "source_issue_id")
    private Integer sourceIssueId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReturnEntryStatus status = ReturnEntryStatus.DRAFT;

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

    @OneToMany(mappedBy = "returnId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ReturnEntryAttachmentJpa> attachments = new ArrayList<>();

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
