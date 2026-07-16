package com.g42.platform.gms.dashboard.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "staff_dashboard_config")
@Data
public class StaffDashboardConfigJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @Column(name = "dashboard_name", nullable = false, length = 100)
    private String dashboardName;

    @Column(name = "layout_config", nullable = false, columnDefinition = "TEXT")
    private String layoutConfig;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
