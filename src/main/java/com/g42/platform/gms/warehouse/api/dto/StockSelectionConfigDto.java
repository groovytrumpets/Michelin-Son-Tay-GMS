package com.g42.platform.gms.warehouse.api.dto;

import com.g42.platform.gms.warehouse.domain.enums.StockAllocationMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Cấu hình kho mặc định và chiến lược chọn lô khi thêm vật tư vào báo giá. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockSelectionConfigDto {
    private Integer configId;
    private Integer defaultWarehouseId;
    /** Chỉ để hiển thị, không dùng khi ghi. */
    private String defaultWarehouseName;
    private StockAllocationMethod allocationMethod;
    private Boolean fallbackToAnyWarehouse;
    private Instant updatedAt;
}
