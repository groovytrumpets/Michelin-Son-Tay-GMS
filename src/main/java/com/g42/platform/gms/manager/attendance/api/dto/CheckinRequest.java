package com.g42.platform.gms.manager.attendance.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class CheckinRequest {

    @NotNull(message = "staffId không được để trống")
    private Integer staffId;

    @NotNull(message = "shiftId không được để trống")
    private Integer shiftId;

    private LocalDate attendanceDate; // null = hôm nay

    private LocalTime checkInTime;    // null = giờ hiện tại

    private String notes;
}
