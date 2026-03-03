package com.g42.platform.gms.service_ticket_management.domain.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.PhotoCategory;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Domain entity representing a Vehicle Condition Photo.
 * Simple POJO following the booking pattern.
 */
@Data
public class VehicleConditionPhoto {
    
    private Integer photoId;
    private Integer serviceTicketId;
    private PhotoCategory category;
    private String photoUrl;
    private String description;
    private LocalDateTime uploadedAt;
    private Integer uploadedBy;
}
