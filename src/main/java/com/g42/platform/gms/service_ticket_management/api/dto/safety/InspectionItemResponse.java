package com.g42.platform.gms.service_ticket_management.api.dto.safety;

import com.g42.platform.gms.service_ticket_management.domain.enums.ItemStatus;
import lombok.Data;

@Data
public class InspectionItemResponse {
    
    private Integer itemId;
    private Integer workCategoryId;    // FK → work_category (default 13), null nếu custom
    private Integer customCategoryId;  // FK → ticket_custom_category, null nếu default
    private String categoryName;       // Tên hiển thị (từ work_category hoặc ticket_custom_category)
    private ItemStatus itemStatus;
    private String advisorNote;
}