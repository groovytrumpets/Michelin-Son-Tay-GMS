package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;

import java.util.List;
import java.util.Optional;

/**
 * Domain port — persistence contract cho Inventory.
 * Service chỉ phụ thuộc vào interface này, không biết đến JpaRepo.
 */
public interface InventoryRepo {

    Optional<InventoryJpa> findByWarehouseAndItem(Integer warehouseId, Integer itemId);

    /** SELECT FOR UPDATE — dùng khi cần atomic update */
    Optional<InventoryJpa> findByWarehouseAndItemWithLock(Integer warehouseId, Integer itemId);

    List<InventoryJpa> findByWarehouse(Integer warehouseId);

    List<InventoryJpa> findLowStock(Integer warehouseId);

    InventoryJpa save(InventoryJpa inventory);
}
