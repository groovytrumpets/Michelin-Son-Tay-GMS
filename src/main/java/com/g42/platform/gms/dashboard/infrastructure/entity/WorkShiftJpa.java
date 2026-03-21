package com.g42.platform.gms.dashboard.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity(name = "DashboardWorkShiftJpa")
@Table(name = "work_shift")
@Data
public class WorkShiftJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shift_id")
    private Integer shiftId;

    @Column(name = "shift_name", nullable = false, length = 50)
    private String shiftName;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
