package com.g42.platform.gms.service_ticket_management.domain.projection;

import com.g42.platform.gms.service_ticket_management.domain.enums.ItemStatus;

/**
 * Projection interface for safety inspection item with category name from JOIN query.
 */
public interface SafetyInspectionItemWithCategory {
    Integer getItemId();
    Integer getInspectionId();
    Integer getWorkCategoryId();
    Integer getCustomCategoryId();
    ItemStatus getItemStatus();
    String getAdvisorNote();
    String getCategoryName();  // From work_category OR ticket_custom_category
}
