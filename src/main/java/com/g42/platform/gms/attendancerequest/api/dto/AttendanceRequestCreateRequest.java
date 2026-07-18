package com.g42.platform.gms.attendancerequest.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class AttendanceRequestCreateRequest {

    @NotBlank(message = "Loại yêu cầu không được để trống")
    private String requestType;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    private LocalDate endDate;

    private Integer shiftId;

    private LocalTime checkInTime;

    private LocalTime checkOutTime;

    @NotBlank(message = "Vui lòng nhập lý do")
    private String reason;
}
