package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.InventoryJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InventoryRepoImpl implements InventoryRepo {

    private final InventoryJpaRepo jpaRepo;

    @Override
    public Optional<InventoryJpa> findByWarehouseAndItem(Integer warehouseId, Integer itemId) {
        return jpaRepo.findByWarehouseIdAndItemId(warehouseId, itemId);
    }

    @Override
    public Optional<InventoryJpa> findByWarehouseAndItemWithLock(Integer warehouseId, Integer itemId) {
        return jpaRepo.findByWarehouseIdAndItemIdWithLock(warehouseId, itemId);
    }

    @Override
    public List<InventoryJpa> findByWarehouse(Integer warehouseId) {
        return jpaRepo.findByWarehouseId(warehouseId);
    }

    @Override
    public List<InventoryJpa> findLowStock(Integer warehouseId) {
        return jpaRepo.findLowStockByWarehouse(warehouseId);
    }

    @Override
    public InventoryJpa save(InventoryJpa inventory) {
        return jpaRepo.save(inventory);
    }
}
