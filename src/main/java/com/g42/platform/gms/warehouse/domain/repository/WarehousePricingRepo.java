package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.WarehousePricing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface WarehousePricingRepo {

    List<WarehousePricing> findActiveByWarehouse(Integer warehouseId);

    Page<WarehousePricing> search(Integer warehouseId,
                                  Boolean isActive,
                                  String search,
                                  Pageable pageable);

    Optional<WarehousePricing> findActiveByWarehouseAndItem(Integer warehouseId, Integer itemId);

    /** Lấy pricing theo item + warehouse (không filter active) — dùng cho PricingService */
    Optional<WarehousePricing> findByItemIdAndWarehouseId(Integer itemId, Integer warehouseId);

    Optional<WarehousePricing> findById(Integer pricingId);

    WarehousePricing save(WarehousePricing pricing);
}
