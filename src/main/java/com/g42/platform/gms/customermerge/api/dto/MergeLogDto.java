package com.g42.platform.gms.customermerge.api.dto;

import lombok.Data;

@Data
public class MergeLogDto {
    private Integer mergeLogId;
    private String mergeType;
    private Integer keptCustomerId;
    private String keptCustomerName;
    private String keptCustomerCode;
    private String mergedCustomerIds;
    private Integer keptVehicleId;
    private String mergedVehicleIds;
    private String summary;
    private String note;
    private Integer mergedByStaffId;
    private String mergedByStaffName;
    private String mergedAt;
}
