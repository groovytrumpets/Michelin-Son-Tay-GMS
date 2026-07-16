package com.g42.platform.gms.dashboard.api.dto;

import lombok.Data;

@Data
public class StaffDashboardConfigRequest {
    private String dashboardName;
    private String layoutConfig;
    private Boolean isActive;
}
