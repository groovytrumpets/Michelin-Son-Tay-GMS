package com.g42.platform.gms.warehouse.api.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ReturnEntryItemResponse {
    private Integer returnItemId;
    private Integer itemId;
    private Integer sourceIssueItemId;
    private Integer entryItemId;
    private String itemName;
    private Integer quantity;
    private String conditionNote;
    private List<String> attachmentUrls;
}
