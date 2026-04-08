package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryJpaRepo extends JpaRepository<InventoryJpa, Integer> {

    Optional<InventoryJpa> findByWarehouseIdAndItemId(Integer warehouseId, Integer itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM InventoryJpa i WHERE i.warehouseId = :warehouseId AND i.itemId = :itemId")
    Optional<InventoryJpa> findByWarehouseIdAndItemIdWithLock(
            @Param("warehouseId") Integer warehouseId,
            @Param("itemId") Integer itemId);

    List<InventoryJpa> findByWarehouseId(Integer warehouseId);

    @Query("SELECT i FROM InventoryJpa i WHERE i.warehouseId = :warehouseId AND (i.quantity - i.reservedQuantity) <= i.minStockLevel")
    List<InventoryJpa> findLowStockByWarehouse(@Param("warehouseId") Integer warehouseId);
}
