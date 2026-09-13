package com.g42.platform.gms.warehouse.api.internal;


import com.g42.platform.gms.common.util.Qty;
import org.apache.commons.lang3.tuple.Pair;
import com.g42.platform.gms.marketing.service_catalog.domain.entity.Service;
import com.g42.platform.gms.warehouse.api.dto.CatalogItemDto;
import com.g42.platform.gms.warehouse.api.dto.HomeCatalogItemInfoDto;
import com.g42.platform.gms.warehouse.api.dto.HomeStockLocationDto;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface WarehouseInternalApi {
    CatalogItemDto getItemInfo(Integer itemId);

    void updateCatalogBlogService(Service serviceSaved, Integer catalogId);

    /** Gỡ liên kết service khỏi mọi catalog item đang trỏ tới nó — dùng trước khi xóa service. */
    void clearCatalogService(Long serviceId);

    void updateInventoryEstimateAllocation(Integer itemId, Integer warehouseId,BigDecimal quantity);

    Integer findCodeByCategoryCode(String categoryCode);

    List<Warehouse> findAllById(List<Integer> warehouseIds);

    CatalogItem findCatalogById(Integer getItemId);

    Inventory findInventoryByWarehouseIdAndItemIds(Integer warehouseId, Integer itemId);

    Inventory findItemAvailableInOtherWarehouse(Integer itemId, int i);

    BigDecimal findItemPricing(Integer itemId, Integer integer, BigDecimal price);

    Pair<Integer,String> getReturnStatusByAlloId(Integer allocationId);

    BigDecimal findLatesFallBackPrice(Integer itemId, Integer warehouseId);
    BigDecimal findLatesFallBackPriceWholesale(Integer itemId, Integer warehouseId);

    /** Thông tin hạng mục/hãng/dòng/xe tương thích/tồn kho khả dụng cho trang public, batch theo itemIds. */
    Map<Integer, HomeCatalogItemInfoDto> getHomeCatalogInfoByItemIds(java.util.Set<Integer> itemIds);

    /** Danh sách kho/cửa hàng còn hàng (available.signum() > 0) của một item cho trang chi tiết public. */
    List<HomeStockLocationDto> getHomeStockLocations(Integer itemId);
}
