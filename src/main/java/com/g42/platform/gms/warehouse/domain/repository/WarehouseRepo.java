package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface WarehouseRepo {
    Page<CatalogItem> getListOfCatalogItems(int page, int size, CatalogItemType itemType, Boolean isActive, String search, Integer brandId, Integer productLineId, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String sortBy);
}
