package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import com.g42.platform.gms.service_ticket_management.domain.enums.PhotoCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for uploading vehicle condition photos.
 * Supports all photo categories including DAMAGE.
 */
@Data
public class PhotoUploadRequest {
    
    @NotBlank(message = "Mã service ticket là bắt buộc")
    private String ticketCode;
    
    @NotNull(message = "Category ảnh là bắt buộc")
    private PhotoCategory category;
    
    // Required for DAMAGE category
    @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
    private String description;
    
    // Uploaded by (receptionist/staff ID who performs check-in)
    @NotNull(message = "Thiếu thông tin người upload (uploadedBy)")
    private Integer uploadedBy;
    
    // MultipartFile will be passed separately in controller
}
