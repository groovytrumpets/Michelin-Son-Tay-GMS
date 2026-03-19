package com.g42.platform.gms.manager.schedule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StaffScheduleRequest {
    @NotNull
    private Integer staffId;
    @NotNull
    private LocalDate workDate;
    @NotNull
    private Integer shiftId;
    private String notes;
}
