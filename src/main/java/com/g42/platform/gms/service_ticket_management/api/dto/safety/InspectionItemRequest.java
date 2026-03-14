package com.g42.platform.gms.service_ticket_management.api.dto.safety;

import com.g42.platform.gms.service_ticket_management.domain.enums.ItemStatus;
import lombok.Data;

@Data
public class InspectionItemRequest {
    
    private Integer workCategoryId;  // Foreign key to work_category table
    private ItemStatus itemStatus;
}