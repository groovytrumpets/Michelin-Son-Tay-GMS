package com.g42.platform.gms.dashboard.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "kpi_monthly_result")
@Data
public class KpiMonthlyResultJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "result_id")
    private Integer resultId;

    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @Column(name = "period_month", nullable = false, length = 7)
    private String periodMonth; // e.g. "2026-03"

    @Column(name = "attendance_score", nullable = false)
    private Double attendanceScore;

    @Column(name = "completion_score", nullable = false)
    private Double completionScore;

    @Column(name = "satisfaction_score", nullable = false)
    private Double satisfactionScore;

    @Column(name = "quality_score", nullable = false)
    private Double qualityScore;

    @Column(name = "total_kpi_score", nullable = false)
    private Double totalKpiScore;

    @Column(name = "calculated_at")
    private LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        calculatedAt = LocalDateTime.now();
    }
}
