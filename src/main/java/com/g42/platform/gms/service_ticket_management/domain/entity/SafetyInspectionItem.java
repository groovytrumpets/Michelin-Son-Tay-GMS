package com.g42.platform.gms.service_ticket_management.domain.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.ItemStatus;
import lombok.Data;

/**
 * Domain entity representing a safety checklist item in a safety inspection.
 */
@Data
public class SafetyInspectionItem {
    
    private Integer itemId;
    private Integer inspectionId;
    private Integer workCategoryId;  // Foreign key to work_category table
    private String categoryName;     // Vietnamese display name from work_category (for response only)
    private ItemStatus itemStatus;
    private String notes;
}
