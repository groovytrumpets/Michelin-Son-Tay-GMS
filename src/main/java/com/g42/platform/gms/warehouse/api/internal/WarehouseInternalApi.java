package com.g42.platform.gms.warehouse.api.internal;

import com.g42.platform.gms.marketing.service_catalog.domain.entity.Service;
import com.g42.platform.gms.warehouse.api.dto.CatalogItemDto;

public interface WarehouseInternalApi {
    CatalogItemDto getItemInfo(Integer itemId);

    void updateCatalogBlogService(Service serviceSaved, Integer catalogId);
}
