package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;

import java.util.List;
import java.util.Optional;

public interface WarehousePricingRepo {

    List<WarehousePricingJpa> findActiveByWarehouse(Integer warehouseId);

    Optional<WarehousePricingJpa> findActiveByWarehouseAndItem(Integer warehouseId, Integer itemId);

    /** Lấy pricing theo item + warehouse (không filter active) — dùng cho PricingService */
    Optional<WarehousePricingJpa> findByItemIdAndWarehouseId(Integer itemId, Integer warehouseId);

    Optional<WarehousePricingJpa> findById(Integer pricingId);

    WarehousePricingJpa save(WarehousePricingJpa pricing);
}
