package com.g42.platform.gms.warehouse.domain.entity;

import lombok.Data;

@Data
public class ReturnEntryItem {
    private Integer returnItemId;
    private Integer returnId;
    private Integer itemId;
    private Integer allocationId;
    private Integer sourceIssueItemId;
    private Integer entryItemId;
    private Integer quantity;
    private String conditionNote;
    private boolean exchangeItem;
}
