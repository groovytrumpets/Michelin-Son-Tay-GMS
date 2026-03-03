package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request DTO for looking up booking by booking code.
 * Used in check-in flow to retrieve booking information.
 */
@Data
public class BookingLookupRequest {
    
    @NotBlank(message = "Mã booking là bắt buộc")
    @Pattern(regexp = "^BK_[23456789ABCDEFGHJKMNPQRSTUVWXYZ]{6}$", 
             message = "Mã booking không hợp lệ (format: BK_XXXXXX)")
    private String bookingCode;
}
