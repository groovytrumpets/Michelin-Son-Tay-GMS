package com.g42.platform.gms.manager.attendance.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalTime;

@Data
public class UpdateAttendanceRequest {

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime checkInTime;

    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime checkOutTime;

    /** Nếu null → tự động set "Edited by manager" */
    private String notes;
}
