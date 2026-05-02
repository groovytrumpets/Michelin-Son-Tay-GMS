package com.g42.platform.gms.warehouse.api.internal;

import org.apache.commons.lang3.tuple.Pair;
import com.g42.platform.gms.marketing.service_catalog.domain.entity.Service;
import com.g42.platform.gms.warehouse.api.dto.CatalogItemDto;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface WarehouseInternalApi {
    CatalogItemDto getItemInfo(Integer itemId);

    void updateCatalogBlogService(Service serviceSaved, Integer catalogId);

    void updateInventoryEstimateAllocation(Integer itemId, Integer warehouseId,Integer quantity);

    Integer findCodeByCategoryCode(String categoryCode);

    List<Warehouse> findAllById(List<Integer> workCategoryIds);

    CatalogItem findCatalogById(Integer getItemId);

    Inventory findInventoryByWarehouseIdAndItemIds(Integer warehouseId, Integer itemId);

    Inventory findItemAvailableInOtherWarehouse(Integer itemId, int i);

    BigDecimal findItemPricing(Integer itemId, Integer integer, BigDecimal price);

    Pair<Integer,String> getReturnStatusByAlloId(Integer allocationId);
}
