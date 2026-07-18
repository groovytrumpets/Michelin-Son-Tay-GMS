package com.g42.platform.gms.attendancerequest.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequest {
    private Integer requestId;
    private Integer staffId;
    private String staffName;
    private String requestType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer shiftId;
    private String shiftName;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String reason;
    private String status;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
