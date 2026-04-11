package com.g42.platform.gms.warehouse.infrastructure.specification;

import com.g42.platform.gms.warehouse.api.dto.CatalogItemDto;
import com.g42.platform.gms.warehouse.api.internal.WarehouseInternalApi;
import com.g42.platform.gms.warehouse.app.service.inventory.InventoryService;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseErrorCode;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseException;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.mapper.CatalogItemJpaMapper;
import com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WarehouseInternalApiImpl implements WarehouseInternalApi {
    @Autowired
    private CatalogItemJpaRepo catalogItemRepo;
    @Autowired
    private CatalogItemJpaMapper catalogItemJpaMapper;
    @Autowired
    private InventoryService inventoryService;


    @Override
    public CatalogItemDto getItemInfo(Integer itemId) {
        CatalogItemJpa catalogItemJpa = catalogItemRepo.findById(itemId).orElse(null);
        return catalogItemJpaMapper.toDto(catalogItemJpa);
    }

    @Override
    public void updateCatalogBlogService(com.g42.platform.gms.marketing.service_catalog.domain.entity.Service serviceSaved, Integer catalogId) {
        CatalogItemJpa catalogItemJpa = catalogItemRepo.findById(catalogId).orElse(null);
        if (catalogItemJpa == null) {
            throw new WarehouseException("Catalog 404", WarehouseErrorCode.CATALOG_404);
        }
        if (catalogItemJpa.getServiceId() != null) {
            throw new WarehouseException("Catalog already have service id", WarehouseErrorCode.CATALOG_404);
        }
        catalogItemJpa.setServiceId(serviceSaved.getServiceId());
        System.out.println("DEBUG: catalogItem ID: " + catalogItemJpa.getServiceId()+" Saved wth serviceId: " + serviceSaved.getServiceId());
    }

    @Override
    public void updateInventoryEstimateAllocation(Integer itemId, Integer warehouseId, Integer quantity) {
        inventoryService.updateInventoryByEstimate(itemId,warehouseId,quantity);
    }
}
