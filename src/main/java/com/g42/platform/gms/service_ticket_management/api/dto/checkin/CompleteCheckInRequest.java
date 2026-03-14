package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for completing check-in process.
 * Finalizes service ticket creation and marks data as immutable.
 */
@Data
public class CompleteCheckInRequest {
    
    @NotBlank(message = "Mã service ticket là bắt buộc")
    private String ticketCode;
    
    @Size(max = 1000, message = "Ghi chú không được quá 1000 ký tự")
    private String checkInNotes;
}
