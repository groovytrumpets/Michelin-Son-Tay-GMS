package com.g42.platform.gms.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GoogleConnectionStatusResponse {
    private boolean configured;
    private boolean connected;
    private String connectedEmail;
    private boolean ga4PropertyConfigured;
    private boolean searchConsoleSiteConfigured;
    private String updatedAt;
}
