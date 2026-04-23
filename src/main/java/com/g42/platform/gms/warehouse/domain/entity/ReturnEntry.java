package com.g42.platform.gms.warehouse.domain.entity;

import com.g42.platform.gms.warehouse.domain.enums.ReturnEntryStatus;
import com.g42.platform.gms.warehouse.domain.enums.ReturnType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReturnEntry {
    private Integer returnId;
    private String returnCode;
    private Integer warehouseId;
    private String returnReason;
    private ReturnType returnType;
    private Integer sourceIssueId;
    private ReturnEntryStatus status;
    private Integer confirmedBy;
    private LocalDateTime confirmedAt;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReturnEntryItem> items;
}
