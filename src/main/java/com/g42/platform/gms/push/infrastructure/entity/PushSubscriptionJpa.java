package com.g42.platform.gms.push.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Một Web Push subscription = một thiết bị/trình duyệt của một nhân viên.
 * Bảng staff_push_subscription (xem docs/migration_push_subscription.sql).
 */
@Entity
@Table(name = "staff_push_subscription")
@Data
public class PushSubscriptionJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @Column(name = "endpoint", nullable = false, length = 1000)
    private String endpoint;

    @Column(name = "p256dh", nullable = false)
    private String p256dh;

    @Column(name = "auth", nullable = false)
    private String auth;

    @Column(name = "expiration_time")
    private Long expirationTime;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "device_label")
    private String deviceLabel;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        lastUsedAt = now;
        if (active == null) active = true;
    }

    @PreUpdate
    void onUpdate() {
        lastUsedAt = LocalDateTime.now();
    }
}
