package com.g42.platform.gms.service_ticket_management.api.dto.checkin;

import com.g42.platform.gms.service_ticket_management.domain.enums.PhotoCategory;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response DTO after uploading a photo.
 * Contains photo URL and metadata.
 */
@Data
public class PhotoUploadResponse {
    
    private Integer photoId;
    private String photoUrl;
    private PhotoCategory category;
    private String description;
    private LocalDateTime uploadedAt;
    private Integer uploadedBy;
    private String message;  // Success message
}
