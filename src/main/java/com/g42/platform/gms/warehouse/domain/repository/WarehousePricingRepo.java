package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface WarehousePricingRepo {

    List<WarehousePricingJpa> findActiveByWarehouse(Integer warehouseId);

    Page<WarehousePricingJpa> search(Integer warehouseId,
                                     Boolean isActive,
                                     String search,
                                     Pageable pageable);

    Optional<WarehousePricingJpa> findActiveByWarehouseAndItem(Integer warehouseId, Integer itemId);

    /** Lấy pricing theo item + warehouse (không filter active) — dùng cho PricingService */
    Optional<WarehousePricingJpa> findByItemIdAndWarehouseId(Integer itemId, Integer warehouseId);

    Optional<WarehousePricingJpa> findById(Integer pricingId);

    WarehousePricingJpa save(WarehousePricingJpa pricing);
}
