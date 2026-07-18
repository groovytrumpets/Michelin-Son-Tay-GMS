package com.g42.platform.gms.dashboard.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "kpi_config")
@Data
public class KpiConfigJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Integer configId;

    @Column(name = "role_name", nullable = false, length = 50, unique = true)
    private String roleName;

    @Column(name = "attendance_weight", nullable = false)
    private Double attendanceWeight;

    @Column(name = "completion_weight", nullable = false)
    private Double completionWeight;

    @Column(name = "satisfaction_weight", nullable = false)
    private Double satisfactionWeight;

    @Column(name = "quality_weight", nullable = false)
    private Double qualityWeight;

    @Column(name = "target_tickets", nullable = false)
    private Integer targetTickets = 40;

    @Column(name = "target_hours", nullable = false)
    private Double targetHours = 160.0;

    @Column(name = "target_rating", nullable = false)
    private Double targetRating = 4.8;

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
