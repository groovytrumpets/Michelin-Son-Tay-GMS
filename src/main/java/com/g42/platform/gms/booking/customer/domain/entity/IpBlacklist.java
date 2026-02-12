package com.g42.platform.gms.booking.customer.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IpBlacklist {
    private Integer blacklistId;
    private String ipAddress;
    private String reason;
    private Integer blockedBy;
    private LocalDateTime blockedAt;
    private Boolean isActive = true;
    
    public void initializeDefaults() {
        if (blockedAt == null) {
            blockedAt = LocalDateTime.now();
        }
        if (isActive == null) {
            isActive = true;
        }
    }
}
