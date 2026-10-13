package com.g42.platform.gms.warehouse.api.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Response tồn kho — các field nhạy cảm (importPrice) chỉ trả về
 * cho ACCOUNTANT / MANAGER / ADMIN. Advisor thấy sellingPrice.
 * Thủ kho chỉ thấy quantity.
 */
@Data
public class InventoryResponse {
    private Integer inventoryId;
    private Integer warehouseId;
    private Integer itemId;
    private String itemName;
    private String sku;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal reservedQuantity;
    private BigDecimal availableQuantity;

    /**
     * Chỉ có khi xem kèm kho con (includeChildren=true): phần nằm trực tiếp trong kho đang xem.
     * Khi đó quantity/reservedQuantity/availableQuantity là số đã cộng cả kho con cháu.
     */
    private BigDecimal ownQuantity;

    /** Chỉ trả về cho ADVISOR, ACCOUNTANT, MANAGER, ADMIN */
    private BigDecimal sellingPrice;

    /** Chỉ trả về cho ACCOUNTANT, MANAGER, ADMIN */
    private BigDecimal importPrice;
}
