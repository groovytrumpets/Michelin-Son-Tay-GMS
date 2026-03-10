package com.g42.platform.gms.service_ticket_management.infrastructure.projection;

import com.g42.platform.gms.service_ticket_management.domain.enums.ItemStatus;

/**
 * Projection interface for safety inspection item with category name from JOIN query.
 */
public interface SafetyInspectionItemWithCategory {
    Integer getItemId();
    Integer getInspectionId();
    Integer getWorkCategoryId();
    ItemStatus getItemStatus();
    String getNotes();
    String getCategoryName();  // From work_category.category_name
}
