package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.repository.WarehousePricingRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.WarehousePricingJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WarehousePricingRepoImpl implements WarehousePricingRepo {

    private final WarehousePricingJpaRepo jpaRepo;

    @Override
    public List<WarehousePricingJpa> findActiveByWarehouse(Integer warehouseId) {
        return jpaRepo.findByWarehouseIdAndIsActiveTrueOrderByItemId(warehouseId);
    }

    @Override
    public Optional<WarehousePricingJpa> findActiveByWarehouseAndItem(Integer warehouseId, Integer itemId) {
        return jpaRepo.findByWarehouseIdAndItemIdAndIsActiveTrue(warehouseId, itemId);
    }

    @Override
    public Optional<WarehousePricingJpa> findById(Integer pricingId) {
        return jpaRepo.findById(pricingId);
    }

    @Override
    public WarehousePricingJpa save(WarehousePricingJpa pricing) {
        return jpaRepo.save(pricing);
    }
}
