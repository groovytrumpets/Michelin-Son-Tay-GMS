package com.g42.platform.gms.warehouse.api.internal;

import com.g42.platform.gms.marketing.service_catalog.domain.entity.Service;
import com.g42.platform.gms.warehouse.api.dto.CatalogItemDto;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;

import java.util.List;
import java.util.Map;

public interface WarehouseInternalApi {
    CatalogItemDto getItemInfo(Integer itemId);

    void updateCatalogBlogService(Service serviceSaved, Integer catalogId);

    void updateInventoryEstimateAllocation(Integer itemId, Integer warehouseId,Integer quantity);

    Integer findCodeByCategoryCode(String categoryCode);

    List<Warehouse> findAllById(List<Integer> workCategoryIds);
}
