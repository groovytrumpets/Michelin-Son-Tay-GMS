package com.g42.platform.gms.warehouse.api.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ServiceSuggestion {
    private Integer ruleId;
    private String vehicleTypePattern;
    private Integer kmThreshold;
    private List<Integer> suggestedItemIds;
    private String reason;
}
