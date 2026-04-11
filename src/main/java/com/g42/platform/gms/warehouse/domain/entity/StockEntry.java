package com.g42.platform.gms.warehouse.domain.entity;

import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockEntry {
    private Integer entryId;
    private String entryCode;
    private Integer warehouseId;
    private Integer itemId;
    private String itemName;
    private Integer quantity;
    private BigDecimal importPrice;
    private String supplierName;
    private LocalDate entryDate;
    private StockEntryStatus status;
    private String notes;
    private Integer confirmedBy;
    private LocalDateTime confirmedAt;
    private Integer createdBy;
    private LocalDateTime createdAt;
    private List<String> attachmentUrls;
}
