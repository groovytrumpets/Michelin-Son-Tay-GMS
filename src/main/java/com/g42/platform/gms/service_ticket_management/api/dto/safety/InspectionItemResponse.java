package com.g42.platform.gms.service_ticket_management.api.dto.safety;

import com.g42.platform.gms.service_ticket_management.domain.enums.ItemStatus;
import lombok.Data;

@Data
public class InspectionItemResponse {
    
    private Integer itemId;
    private Integer workCategoryId;  // Foreign key to work_category table
    private String categoryName;     // Vietnamese display name from work_category
    private ItemStatus itemStatus;
    private String notes;
}