package com.g42.platform.gms.customermerge.api.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MergeResultDto {
    private Integer mergeLogId;
    private Integer keptCustomerId;
    private List<Integer> removedCustomerIds = new ArrayList<>();
    private Integer keptVehicleId;
    private List<Integer> removedVehicleIds = new ArrayList<>();
    private String summary;
}
