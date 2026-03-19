package com.g42.platform.gms.manager.schedule.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalTime;

@Data
public class WorkShiftRequest {
    @NotBlank
    private String shiftName;
    @NotNull
    private LocalTime startTime;
    @NotNull
    private LocalTime endTime;
    private Boolean isActive = true;
}
