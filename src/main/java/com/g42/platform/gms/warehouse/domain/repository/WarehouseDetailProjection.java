package com.g42.platform.gms.warehouse.domain.repository;

import java.math.BigDecimal;

public interface WarehouseDetailProjection {
    Integer getWarehouseId();
    String getWarehouseCode();
    String getWarehouseName();
    String getWarehouseAddress();
    Integer getItemId();
    BigDecimal getSellingPrice();
    Integer getQuantity();
    Integer getReservedQuantity();
    Integer getMinStockLevel();
    Integer getMaxStockLevel();
    Integer getAvailableStockLevel();
    String getNotify();
}
