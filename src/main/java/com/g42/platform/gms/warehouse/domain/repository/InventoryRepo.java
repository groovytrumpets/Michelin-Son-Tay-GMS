package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Domain port cho Inventory.
 *
 * Service lớp trên chỉ làm việc với interface này để:
 * - đọc tồn kho theo kho + item
 * - khóa row khi cần cập nhật reservedQuantity an toàn
 * - cập nhật quantity / reservedQuantity trong cùng transaction
 *
 * Ghi chú concurrency:
 * - `findByWarehouseAndItemWithLock(...)` được dùng khi service sẽ tính lại
 *   reserved/available và save ngay sau đó.
 * - Khi không cần ghi, ưu tiên `findByWarehouseAndItem(...)` để tránh lock thừa.
 */
public interface InventoryRepo {

    Optional<Inventory> findByWarehouseAndItem(Integer warehouseId, Integer itemId);

    /** SELECT FOR UPDATE — dùng khi cần atomic update */
    Optional<Inventory> findByWarehouseAndItemWithLock(Integer warehouseId, Integer itemId);

    List<Inventory> findByWarehouse(Integer warehouseId);

    List<Inventory> findLowStock(Integer warehouseId);

    Inventory save(Inventory inventory);
}
