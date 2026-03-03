package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for selecting existing vehicle or creating new vehicle.
 * If vehicleId is provided, use existing vehicle (other fields optional).
 * If vehicleId is null, create new vehicle with provided data (other fields required).
 */
@Data
public class VehicleRequest {
    
    // If provided, use existing vehicle
    private Integer vehicleId;
    
    // Required for new vehicle creation (when vehicleId is null)
    // Optional when vehicleId is provided
    @Pattern(regexp = "^[0-9]{2}[A-Z]{1,2}-[0-9]{4,5}$", 
             message = "Biển số xe không hợp lệ (VD: 29A-12345)")
    private String licensePlate;
    
    @Size(max = 50, message = "Hãng xe không được quá 50 ký tự")
    private String make;
    
    @Size(max = 50, message = "Model xe không được quá 50 ký tự")
    private String model;
    
    @Min(value = 1900, message = "Năm sản xuất phải từ 1900")
    @Max(value = 2100, message = "Năm sản xuất không hợp lệ")
    private Integer year;
    
    @Size(max = 30, message = "Màu xe không được quá 30 ký tự")
    private String color;
    
    @NotNull(message = "Customer ID là bắt buộc")
    private Integer customerId;
    
    @NotNull(message = "Booking ID là bắt buộc")
    private Integer bookingId;
}
