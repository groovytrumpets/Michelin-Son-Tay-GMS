package com.g42.platform.gms.warehouse.api.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReturnEntryItemResponse {
    private Integer returnItemId;
    private Integer itemId;
    private String itemCode;
    private String itemName;
    private Integer allocationId;
    private Integer sourceIssueItemId;
    private Integer entryItemId;
    private String entryCode;
    private String entryLotCode;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String conditionNote;
    private List<String> attachmentUrls;
}
