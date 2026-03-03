package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for recording odometer reading during check-in.
 */
@Data
public class OdometerRequest {
    
    @NotBlank(message = "Mã service ticket là bắt buộc")
    @Pattern(regexp = "^ST_[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{6}$", 
             message = "Mã service ticket không hợp lệ (format: ST_XXXXXX)")
    private String ticketCode;
    
    @NotNull(message = "Số công tơ mét là bắt buộc")
    @Min(value = 1, message = "Số công tơ mét phải lớn hơn 0")
    @Max(value = 9999999, message = "Số công tơ mét không hợp lệ")
    private Integer reading;
    
    @NotNull(message = "Vehicle ID là bắt buộc")
    private Integer vehicleId;
}
