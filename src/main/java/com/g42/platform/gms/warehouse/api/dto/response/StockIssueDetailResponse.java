package com.g42.platform.gms.warehouse.api.dto.response;

import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockIssueDetailResponse {
    private Integer issueId;
    private String issueCode;
    private Integer warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private IssueType issueType;
    private String issueReason;
    private Integer serviceTicketId;
    private String serviceTicketCode;
    private BigDecimal discountRate;
    private StockIssueStatus status;
    private Integer confirmedBy;
    private String confirmedByName;
    private LocalDateTime confirmedAt;
    private Integer createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<IssueItemDetail> items;

    @Data
    public static class IssueItemDetail {
        private Integer issueItemId;
        private String issueItemCode;
        private Integer itemId;
        private String itemName;
        private Integer entryItemId;   // lô nhập tương ứng
        private String entryCode;
        private String entryLotCode;
        private Integer quantity;
        private BigDecimal exportPrice;   // = selling_price của lô
        private BigDecimal importPrice;   // = import_price của lô
        private BigDecimal discountRate;
        private BigDecimal finalPrice;
        private BigDecimal grossProfit;
    }
}
