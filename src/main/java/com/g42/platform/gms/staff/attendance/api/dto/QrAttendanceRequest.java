package com.g42.platform.gms.staff.attendance.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Dùng chung cho cả check-in và check-out qua QR + GPS.
 */
@Getter
@Setter
public class QrAttendanceRequest {

    @NotBlank(message = "qrToken không được để trống")
    private String qrToken;

    @NotNull(message = "latitude không được để trống")
    private Double latitude;

    @NotNull(message = "longitude không được để trống")
    private Double longitude;
}
