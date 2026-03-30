package com.g42.platform.gms.service_ticket_management.api.dto.safety;

import com.g42.platform.gms.service_ticket_management.domain.enums.ItemStatus;
import lombok.Data;

@Data
public class InspectionItemRequest {
    
    /** Dùng cho hạng mục default (13 cái). Null nếu là custom. */
    private Integer workCategoryId;
    /** Dùng cho hạng mục tùy chỉnh. Null nếu là default. */
    private Integer customCategoryId;
    private ItemStatus itemStatus;
}