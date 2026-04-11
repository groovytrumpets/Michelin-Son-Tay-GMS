package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WarehousePricingJpaRepo extends JpaRepository<WarehousePricingJpa, Integer> {

    List<WarehousePricingJpa> findByWarehouseIdAndIsActiveTrueOrderByItemId(Integer warehouseId);

    java.util.Optional<WarehousePricingJpa> findByWarehouseIdAndItemIdAndIsActiveTrue(Integer warehouseId, Integer itemId);
    @Query("""
    select wp from WarehousePricingJpa wp where wp.itemId=:itemId and wp.warehouseId=:warehouseId and wp.isActive=true 
        """)
    WarehousePricingJpa getWarehousePricingJpaByItemIdAndWarehouseId(Integer itemId, Integer warehouseId);
}
