package com.g42.platform.gms.warehouse.domain.repository;

import java.math.BigDecimal;

public interface WarehouseDetailProjection {
    Integer getWarehouseId();
    String getWarehouseCode();
    String getWarehouseName();
    String getWarehouseAddress();
    Integer getItemId();
    BigDecimal getSellingPrice();
    java.math.BigDecimal getQuantity();
    java.math.BigDecimal getReservedQuantity();
    Integer getMinStockLevel();
    Integer getMaxStockLevel();
    java.math.BigDecimal getAvailableStockLevel();
    String getNotify();
}
