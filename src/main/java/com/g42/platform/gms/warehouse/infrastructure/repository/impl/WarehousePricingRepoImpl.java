package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.WarehousePricing;
import com.g42.platform.gms.warehouse.domain.repository.WarehousePricingRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.WarehousePricingJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class WarehousePricingRepoImpl implements WarehousePricingRepo {

    private final WarehousePricingJpaRepo jpaRepo;

    @Override
    public List<WarehousePricing> findActiveByWarehouse(Integer warehouseId) {
        return jpaRepo.findByWarehouseIdAndIsActiveTrueOrderByItemId(warehouseId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Page<WarehousePricing> search(Integer warehouseId,
                                         Boolean isActive,
                                         String search,
                                         Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        return jpaRepo.search(warehouseId, isActive, normalizedSearch, pageable)
                .map(this::toDomain);
    }

    @Override
    public Optional<WarehousePricing> findActiveByWarehouseAndItem(Integer warehouseId, Integer itemId) {
        return jpaRepo.findByWarehouseIdAndItemIdAndIsActiveTrue(warehouseId, itemId)
                .map(this::toDomain);
    }

    @Override
    public Optional<WarehousePricing> findById(Integer pricingId) {
        return jpaRepo.findById(pricingId).map(this::toDomain);
    }

    @Override
    public Optional<WarehousePricing> findByItemIdAndWarehouseId(Integer itemId, Integer warehouseId) {
        return jpaRepo.findByWarehouseIdAndItemIdAndIsActiveTrue(warehouseId, itemId)
                .map(this::toDomain);
    }

    @Override
    public WarehousePricing save(WarehousePricing pricing) {
        WarehousePricingJpa saved = jpaRepo.save(toJpa(pricing));
        return toDomain(saved);
    }

    private WarehousePricing toDomain(WarehousePricingJpa jpa) {
        WarehousePricing domain = new WarehousePricing();
        domain.setPricingId(jpa.getPricingId());
        domain.setWarehouseId(jpa.getWarehouseId());
        domain.setItemId(jpa.getItemId());
        domain.setBasePrice(jpa.getBasePrice());
        domain.setMarkupMultiplier(jpa.getMarkupMultiplier());
        domain.setSellingPrice(jpa.getSellingPrice());
        domain.setEffectiveFrom(jpa.getEffectiveFrom());
        domain.setEffectiveTo(jpa.getEffectiveTo());
        domain.setIsActive(jpa.getIsActive());
        domain.setCreatedAt(jpa.getCreatedAt());
        return domain;
    }

    private WarehousePricingJpa toJpa(WarehousePricing domain) {
        WarehousePricingJpa jpa = new WarehousePricingJpa();
        jpa.setPricingId(domain.getPricingId());
        jpa.setWarehouseId(domain.getWarehouseId());
        jpa.setItemId(domain.getItemId());
        jpa.setBasePrice(domain.getBasePrice());
        jpa.setMarkupMultiplier(domain.getMarkupMultiplier());
        jpa.setSellingPrice(domain.getSellingPrice());
        jpa.setEffectiveFrom(domain.getEffectiveFrom());
        jpa.setEffectiveTo(domain.getEffectiveTo());
        jpa.setIsActive(domain.getIsActive());
        jpa.setCreatedAt(domain.getCreatedAt());
        return jpa;
    }
}
