package com.g42.platform.gms.service_ticket_management.api.dto.technician;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating technician notes.
 * Kỹ thuật viên chỉ được phép cập nhật ghi chú kỹ thuật.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTechnicianNotesRequest {
    
    @Size(max = 255, message = "Ghi chú kỹ thuật không được vượt quá 255 ký tự")
    private String technicianNotes;
}
