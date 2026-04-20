package com.g42.platform.gms.warehouse.api.dto.response;

import com.g42.platform.gms.warehouse.domain.enums.ReturnEntryStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReturnEntryResponse {
    private Integer returnId;
    private String returnCode;
    private Integer warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String returnReason;
    private Integer sourceIssueId;
    private String sourceIssueCode;
    private ReturnEntryStatus status;
    private List<ReturnEntryItemResponse> items;
    private Integer confirmedBy;
    private String confirmedByName;
    private LocalDateTime confirmedAt;
    private Integer createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
}
