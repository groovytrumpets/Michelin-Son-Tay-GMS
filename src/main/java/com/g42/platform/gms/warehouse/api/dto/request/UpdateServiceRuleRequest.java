package com.g42.platform.gms.warehouse.api.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class UpdateServiceRuleRequest {
    private String vehicleTypePattern;
    private Integer kmThreshold;
    private List<Integer> suggestedItemIds;
    private String reason;
    private Boolean isActive;
}
