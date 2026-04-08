package com.g42.platform.gms.warehouse.api.dto.response;

import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockEntryResponse {
    private Integer entryId;
    private String entryCode;
    private Integer warehouseId;
    private String supplierName;
    private LocalDate entryDate;
    private StockEntryStatus status;
    private String notes;
    private List<StockEntryItemResponse> items;
    private List<String> attachments; // danh sách URL ảnh chứng từ
    private Integer confirmedBy;
    private LocalDateTime confirmedAt;
    private Integer createdBy;
    private LocalDateTime createdAt;
}
