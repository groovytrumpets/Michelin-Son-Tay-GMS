package com.g42.platform.gms.attendancerequest.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity(name = "AttendanceRequestJpa")
@Table(name = "attendance_request", schema = "michelin_garage")
public class AttendanceRequestJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @Column(name = "request_type", length = 20, nullable = false)
    private String requestType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "shift_id")
    private Integer shiftId;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "reason", length = 500, nullable = false)
    private String reason;

    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    @Column(name = "reviewed_by")
    private Integer reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "review_note", length = 500)
    private String reviewNote;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
