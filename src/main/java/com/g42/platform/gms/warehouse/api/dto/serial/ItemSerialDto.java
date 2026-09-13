package com.g42.platform.gms.warehouse.api.dto.serial;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ItemSerialDto {
    private Integer serialId;
    private Integer itemId;
    private Integer warehouseId;
    private String warehouseName;
    private Integer entryItemId;
    private String entryCode;
    private LocalDate entryDate;
    private BigDecimal importPrice;
    private String serialCode;
    private String status;
    private Integer estimateItemId;
    private Integer issueId;
    private String attributesJson;
    private String notes;
    private LocalDateTime createdAt;
}
