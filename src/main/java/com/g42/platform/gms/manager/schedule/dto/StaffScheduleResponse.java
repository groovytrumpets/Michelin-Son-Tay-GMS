package com.g42.platform.gms.manager.schedule.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class StaffScheduleResponse {
    private Integer scheduleId;
    private Integer staffId;
    private String staffName;
    private String position;
    private LocalDate workDate;
    private Integer shiftId;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private String notes;
}
