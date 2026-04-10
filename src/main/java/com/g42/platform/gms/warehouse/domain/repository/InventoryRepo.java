package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Domain port — persistence contract cho Inventory.
 * Service chỉ phụ thuộc vào interface này và domain entity Inventory.
 */
public interface InventoryRepo {

    Optional<Inventory> findByWarehouseAndItem(Integer warehouseId, Integer itemId);

    /** SELECT FOR UPDATE — dùng khi cần atomic update */
    Optional<Inventory> findByWarehouseAndItemWithLock(Integer warehouseId, Integer itemId);

    List<Inventory> findByWarehouse(Integer warehouseId);

    List<Inventory> findLowStock(Integer warehouseId);

    Inventory save(Inventory inventory);
}
