package com.g42.platform.gms.manager.attendance.api.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
public class AttendanceCheckinResponse {
    private Integer checkinId;
    private Integer staffId;
    private String staffName;
    private LocalDate attendanceDate;
    private Integer shiftId;
    private String shiftName;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
}
