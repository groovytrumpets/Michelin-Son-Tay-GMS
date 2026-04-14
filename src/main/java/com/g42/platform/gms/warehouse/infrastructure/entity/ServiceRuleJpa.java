package com.g42.platform.gms.warehouse.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_rule")
@Data
public class ServiceRuleJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Integer ruleId;

    @Column(name = "vehicle_type_pattern", nullable = false)
    private String vehicleTypePattern;

    @Column(name = "km_threshold", nullable = false)
    private Integer kmThreshold;

    // JSON column — lưu dạng String, parse trong service
    @Column(name = "suggested_item_ids", columnDefinition = "JSON", nullable = false)
    private String suggestedItemIds;

    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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
