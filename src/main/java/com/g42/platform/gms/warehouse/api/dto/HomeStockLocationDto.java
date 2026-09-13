package com.g42.platform.gms.warehouse.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vị trí kho/cửa hàng còn hàng của một catalog item, phục vụ trang chi tiết phụ tùng public.
 * Không lộ số liệu nội bộ (reserved, min/max stock level) — chỉ số lượng khả dụng.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class HomeStockLocationDto {
    private Integer warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private String warehouseType;
    private String address;
    private java.math.BigDecimal availableQty;
}
