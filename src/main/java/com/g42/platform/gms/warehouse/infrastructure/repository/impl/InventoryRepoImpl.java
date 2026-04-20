package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.Inventory;
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
    public Optional<Inventory> findByWarehouseAndItem(Integer warehouseId, Integer itemId) {
        return jpaRepo.findByWarehouseIdAndItemId(warehouseId, itemId).map(this::toDomain);
    }

    @Override
    public Optional<Inventory> findByWarehouseAndItemWithLock(Integer warehouseId, Integer itemId) {
        return jpaRepo.findByWarehouseIdAndItemIdWithLock(warehouseId, itemId).map(this::toDomain);
    }

    @Override
    public List<Inventory> findByWarehouse(Integer warehouseId) {
        return jpaRepo.findByWarehouseId(warehouseId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Inventory> findLowStock(Integer warehouseId) {
        return jpaRepo.findLowStockByWarehouse(warehouseId).stream().map(this::toDomain).toList();
    }

    @Override
    public Inventory save(Inventory inventory) {
        return toDomain(jpaRepo.save(toJpa(inventory)));
    }

    // ── Mapper ───────────────────────────────────────────────────────────────

    private Inventory toDomain(InventoryJpa jpa) {
        return Inventory.builder()
                .inventoryId(jpa.getInventoryId())
                .warehouseId(jpa.getWarehouseId())
                .itemId(jpa.getItemId())
                .quantity(jpa.getQuantity())
                .reservedQuantity(jpa.getReservedQuantity())
                .minStockLevel(jpa.getMinStockLevel())
                .maxStockLevel(jpa.getMaxStockLevel())
                .lastUpdated(jpa.getLastUpdated())
                .build();
    }

    private InventoryJpa toJpa(Inventory domain) {
        InventoryJpa jpa = new InventoryJpa();
        jpa.setInventoryId(domain.getInventoryId());
        jpa.setWarehouseId(domain.getWarehouseId());
        jpa.setItemId(domain.getItemId());
        jpa.setQuantity(domain.getQuantity() != null ? domain.getQuantity() : 0);
        jpa.setReservedQuantity(domain.getReservedQuantity() != null ? domain.getReservedQuantity() : 0);
        jpa.setMinStockLevel(domain.getMinStockLevel() != null ? domain.getMinStockLevel() : 0);
        jpa.setMaxStockLevel(domain.getMaxStockLevel() != null ? domain.getMaxStockLevel() : 0);
        return jpa;
    }
}
