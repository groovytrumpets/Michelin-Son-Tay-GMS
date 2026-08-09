package com.g42.platform.gms.warehouse.api.dto;

import com.g42.platform.gms.warehouse.domain.enums.StockAllocationMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Kho và lô được hệ thống tự chọn cho một vật tư, theo cấu hình
 * kho mặc định + chiến lược chọn lô.
 *
 * entryItemId null nghĩa là chưa chốt lô: hoặc chiến lược đang là MANUAL,
 * hoặc vật tư không còn lô nào khả dụng.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockSuggestionDto {
    private Integer itemId;
    private Integer warehouseId;
    private String warehouseName;
    private Integer entryItemId;
    private String entryCode;
    private Integer availableQuantity;
    private BigDecimal importPrice;
    private LocalDate entryDate;
    private LocalDate expiryDate;
    private StockAllocationMethod allocationMethod;
    /** Lý do khi không chọn được lô, để hiển thị nhắc người dùng. */
    private String message;
}
