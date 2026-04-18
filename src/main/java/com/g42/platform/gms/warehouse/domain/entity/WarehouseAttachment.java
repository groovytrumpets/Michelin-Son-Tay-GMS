package com.g42.platform.gms.warehouse.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WarehouseAttachment {

    private Integer attachmentId;
    private RefType refType;
    private Integer refId;
    private String fileUrl;
    private Integer uploadedBy;
    private LocalDateTime uploadedAt;

    public enum RefType {
        STOCK_ENTRY,
        STOCK_ISSUE,
        RETURN_ENTRY_ITEM
    }
}
