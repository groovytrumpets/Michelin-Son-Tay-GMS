package com.g42.platform.gms.warehouse.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ServiceRule {
    private Integer ruleId;
    private String vehicleTypePattern;
    private Integer kmThreshold;
    private List<Integer> suggestedItemIds;
    private String reason;
    private Boolean isActive;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
