package com.g42.platform.gms.warehouse.api.dto.response;

import lombok.Data;

@Data
public class ReturnEntryItemResponse {
    private Integer returnItemId;
    private Integer itemId;
    private Integer sourceIssueItemId;
    private String itemName;
    private Integer quantity;
    private String conditionNote;
}
