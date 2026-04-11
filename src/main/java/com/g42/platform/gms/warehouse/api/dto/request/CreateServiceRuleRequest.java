package com.g42.platform.gms.warehouse.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateServiceRuleRequest {

    @NotBlank
    private String vehicleTypePattern;

    @NotNull
    @Min(0)
    private Integer kmThreshold;

    @NotEmpty
    private List<Integer> suggestedItemIds;

    @NotBlank
    private String reason;
}
