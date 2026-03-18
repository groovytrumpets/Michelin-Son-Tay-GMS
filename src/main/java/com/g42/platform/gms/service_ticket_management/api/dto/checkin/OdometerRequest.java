package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Request DTO for recording odometer reading during check-in.
 */
@Data
public class OdometerRequest {
    
    @NotBlank(message = "Mã service ticket là bắt buộc")
    private String ticketCode;
    
    @Max(value = 9999999, message = "Số công tơ mét không hợp lệ")
    private Integer reading;
    
    @NotNull(message = "Vehicle ID là bắt buộc")
    private Integer vehicleId;
}
