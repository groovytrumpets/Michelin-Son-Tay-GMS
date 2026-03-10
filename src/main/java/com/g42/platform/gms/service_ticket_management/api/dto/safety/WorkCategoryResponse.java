package com.g42.platform.gms.service_ticket_management.api.dto.safety;

import lombok.Data;

@Data
public class WorkCategoryResponse {
    private Integer id;
    private String categoryCode;
    private String categoryName;
    private Integer displayOrder;
    private Boolean isActive;
    private String categoryType;
}