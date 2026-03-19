package com.g42.platform.gms.manager.schedule.dto;

import lombok.Data;

import java.time.LocalTime;

@Data
public class WorkShiftResponse {
    private Integer shiftId;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isActive;
}
