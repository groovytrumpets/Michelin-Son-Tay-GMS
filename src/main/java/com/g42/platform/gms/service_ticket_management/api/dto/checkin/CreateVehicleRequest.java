package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for creating a new vehicle.
 * Used when customer doesn't have a vehicle in the system yet.
 * 
 * Validation rules:
 * - licensePlate: BẮT BUỘC (unique identifier)
 * - make, model, year: OPTIONAL (có thể null nếu chưa biết thông tin)
 */
@Data
public class CreateVehicleRequest {
    
    @NotNull(message = "Customer ID là bắt buộc")
    private Integer customerId;
    
    @NotBlank(message = "Biển số xe là bắt buộc")
    private String licensePlate;
    
    // Optional - Hãng xe (VD: Toyota, Honda)
    @Size(max = 50, message = "Hãng xe không được quá 50 ký tự")
    private String make;
    
    // Optional - Dòng xe (VD: Camry, Civic)
    @Size(max = 50, message = "Model xe không được quá 50 ký tự")
    private String model;
    
    // Optional - Năm sản xuất
    @Min(value = 1900, message = "Năm sản xuất phải từ 1900 trở lên")
    @Max(value = 2100, message = "Năm sản xuất không hợp lệ")
    private Integer year;
}
