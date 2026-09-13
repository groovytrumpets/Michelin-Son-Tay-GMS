package com.g42.platform.gms.vehicle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for updating an existing vehicle's info
 * (used by staff when editing a customer's profile).
 */
@Data
public class VehicleUpdateRequest {

    @NotBlank(message = "Biển số xe là bắt buộc")
    private String licensePlate;

    @Size(max = 50, message = "Hãng xe không được quá 50 ký tự")
    private String brand;

    @Size(max = 50, message = "Model xe không được quá 50 ký tự")
    private String model;

    @Min(value = 1900, message = "Năm sản xuất phải từ 1900 trở lên")
    @Max(value = 2100, message = "Năm sản xuất không hợp lệ")
    private Integer manufactureYear;
}
