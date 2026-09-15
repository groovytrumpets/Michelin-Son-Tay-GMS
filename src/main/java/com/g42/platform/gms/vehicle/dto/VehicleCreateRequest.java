package com.g42.platform.gms.vehicle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for adding a vehicle to a customer from the staff
 * vehicle management screen (/vehicle-management).
 */
@Data
public class VehicleCreateRequest {

    @NotNull(message = "Khách hàng là bắt buộc")
    private Integer customerId;

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
