package com.g42.platform.gms.service_ticket_management.domain.entity;

import lombok.Data;

@Data
public class WorkCategory {
    private Integer id;
    private String categoryCode;
    private String categoryName;
    private Integer displayOrder;
    private Boolean isActive;
    private Boolean isDefault;
    private String advisorNote;
    
    public void initializeDefaults() {
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.isDefault == null) {
            this.isDefault = false;
        }
    }
}