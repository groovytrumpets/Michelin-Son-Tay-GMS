package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.api.dto.WarehouseDetailDto;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface WarehouseRepo {
    Page<CatalogItem> getListOfCatalogItems(int page, int size, CatalogItemType itemType, Boolean isActive, String search, Integer brandId, Integer productLineId, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String sortBy);

    List<WarehouseDetailProjection> getWarehouseDetailsByItemIds(Set<Integer> itemIds);

    List<WarehouseDetailDto> getWarehouseDetailsByItemId(Integer itemId);

    List<Warehouse> getAllWarehouse();

    Optional<Warehouse> findById(Integer warehouseId);
}
