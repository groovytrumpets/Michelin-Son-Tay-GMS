package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for creating a new vehicle.
 * Used when customer doesn't have a vehicle in the system yet.
 */
@Data
public class CreateVehicleRequest {
    
    @NotNull(message = "Customer ID là bắt buộc")
    private Integer customerId;
    
    @NotBlank(message = "Biển số xe là bắt buộc")
    @Pattern(regexp = "^[0-9]{2}[A-Z]{1,2}-[0-9]{4,5}$", 
             message = "Biển số xe không hợp lệ (VD: 29A-12345)")
    private String licensePlate;
    
    @NotBlank(message = "Hãng xe là bắt buộc")
    @Size(max = 50, message = "Hãng xe không được quá 50 ký tự")
    private String make;
    
    @NotBlank(message = "Model xe là bắt buộc")
    @Size(max = 50, message = "Model xe không được quá 50 ký tự")
    private String model;
    
    @NotNull(message = "Năm sản xuất là bắt buộc")
    @Min(value = 1900, message = "Năm sản xuất phải từ 1900 trở lên")
    @Max(value = 2100, message = "Năm sản xuất không hợp lệ")
    private Integer year;
}
