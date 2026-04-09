package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehousePricingJpaRepo extends JpaRepository<WarehousePricingJpa, Integer> {

    List<WarehousePricingJpa> findByWarehouseIdAndIsActiveTrueOrderByItemId(Integer warehouseId);

    java.util.Optional<WarehousePricingJpa> findByWarehouseIdAndItemIdAndIsActiveTrue(Integer warehouseId, Integer itemId);

    WarehousePricingJpa getWarehousePricingJpaByItemIdAndWarehouseId(Integer itemId, Integer warehouseId);
}
