package com.g42.platform.gms.booking.customer.entity;

import com.g42.platform.gms.auth.entity.StaffProfile;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "ip_blacklist", indexes = {
    @Index(name = "idx_ip", columnList = "ip_address"),
    @Index(name = "idx_active", columnList = "is_active")
})
@Data
public class IpBlacklist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blacklist_id")
    private Integer blacklistId;

    @Column(name = "ip_address", nullable = false, unique = true, length = 45)
    private String ipAddress; // Hỗ trợ cả IPv4 và IPv6

    @Column(name = "reason", length = 255)
    private String reason;

    @ManyToOne
    @JoinColumn(name = "blocked_by")
    private StaffProfile blockedBy;

    @Column(name = "blocked_at", updatable = false)
    private LocalDateTime blockedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        if (blockedAt == null) {
            blockedAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}
