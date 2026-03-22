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
    /** FK → work_category (13 default). Null nếu là hạng mục tùy chỉnh. */
    private Integer workCategoryId;
    /** FK → ticket_custom_category. Null nếu là hạng mục default. */
    private Integer customCategoryId;
    private String categoryName;     // Populated from JOIN query (for response only)
    private ItemStatus itemStatus;
    private String advisorNote;
}
