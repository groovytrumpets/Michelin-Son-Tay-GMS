package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.domain.repository.PartCatalogRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class PartCatalogRepoImpl implements PartCatalogRepo {

    private final CatalogItemJpaRepo jpaRepo;

    @Override
    public List<CatalogItem> findAllParts() {
        return jpaRepo.findByItemType(CatalogItemType.PART).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<CatalogItem> searchParts(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        return jpaRepo.findByItemType(CatalogItemType.PART).stream()
                .filter(c -> normalized.isEmpty()
                        || containsIgnoreCase(c.getItemName(), normalized)
                        || containsIgnoreCase(c.getSku(), normalized)
                        || containsIgnoreCase(c.getPartNumber(), normalized)
                        || containsIgnoreCase(c.getBarcode(), normalized))
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<CatalogItem> findAllPartsByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpaRepo.findByItemTypeAndItemIdIn(CatalogItemType.PART, ids).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsBySku(String sku) {
        return jpaRepo.existsBySku(sku);
    }

    @Override
    public CatalogItem save(CatalogItem item) {
        CatalogItemJpa saved = jpaRepo.save(toJpa(item));
        return toDomain(saved);
    }

    @Override
    public Map<Integer, String> findNamesByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return jpaRepo.findByItemTypeAndItemIdIn(CatalogItemType.PART, ids).stream()
                .collect(Collectors.toMap(CatalogItemJpa::getItemId, CatalogItemJpa::getItemName));
    }

    private boolean containsIgnoreCase(String value, String keywordLower) {
        return value != null && value.toLowerCase().contains(keywordLower);
    }

    private CatalogItem toDomain(CatalogItemJpa jpa) {
        CatalogItem domain = new CatalogItem();
        domain.setItemId(jpa.getItemId());
        domain.setItemName(jpa.getItemName());
        domain.setItemType(jpa.getItemType());
        domain.setIsActive(jpa.getIsActive());
        domain.setWarrantyDurationMonths(jpa.getWarrantyDurationMonths());
        domain.setServiceServiceId(jpa.getServiceId());
        domain.setSku(jpa.getSku());
        domain.setPrice(jpa.getPrice());
        domain.setShowPrice(jpa.getShowPrice());
        domain.setDescription(jpa.getDescription());
        domain.setImageUrl(jpa.getImageUrl());
        domain.setUnit(jpa.getUnit());
        domain.setComboDurationMonths(jpa.getComboDurationMonths());
        domain.setComboDescription(jpa.getComboDescription());
        domain.setIsRecurring(jpa.getIsRecurring());
        domain.setBrandId(jpa.getBrandId());
        domain.setProductLineId(jpa.getProductLineId());
        domain.setMadeIn(jpa.getMadeIn());
        domain.setTaxRuleId(jpa.getTaxRuleId());
        domain.setWorkCategoryId(jpa.getWorkCategoryId());
        domain.setPartNumber(jpa.getPartNumber());
        domain.setBarcode(jpa.getBarcode());
        domain.setColor(jpa.getColor());
        return domain;
    }

    private CatalogItemJpa toJpa(CatalogItem domain) {
        CatalogItemJpa jpa = new CatalogItemJpa();
        jpa.setItemId(domain.getItemId());
        jpa.setItemName(domain.getItemName());
        jpa.setItemType(domain.getItemType());
        jpa.setIsActive(domain.getIsActive());
        jpa.setWarrantyDurationMonths(domain.getWarrantyDurationMonths());
        jpa.setServiceId(domain.getServiceServiceId());
        jpa.setSku(domain.getSku());
        jpa.setPrice(domain.getPrice());
        jpa.setShowPrice(domain.getShowPrice());
        jpa.setDescription(domain.getDescription());
        jpa.setImageUrl(domain.getImageUrl());
        jpa.setUnit(domain.getUnit());
        jpa.setComboDurationMonths(domain.getComboDurationMonths());
        jpa.setComboDescription(domain.getComboDescription());
        jpa.setIsRecurring(domain.getIsRecurring());
        jpa.setBrandId(domain.getBrandId());
        jpa.setProductLineId(domain.getProductLineId());
        jpa.setMadeIn(domain.getMadeIn());
        jpa.setTaxRuleId(domain.getTaxRuleId());
        jpa.setWorkCategoryId(domain.getWorkCategoryId());
        jpa.setPartNumber(domain.getPartNumber());
        jpa.setBarcode(domain.getBarcode());
        jpa.setColor(domain.getColor());
        return jpa;
    }
}
