package com.g42.platform.gms.warehouse.infrastructure.specification;

import com.g42.platform.gms.warehouse.api.dto.CatalogItemDto;
import com.g42.platform.gms.warehouse.api.internal.WarehouseInternalApi;
import com.g42.platform.gms.warehouse.app.service.inventory.InventoryService;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseErrorCode;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseException;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WorkCategoryJpaEntity;
import com.g42.platform.gms.warehouse.infrastructure.mapper.CatalogItemJpaMapper;
import com.g42.platform.gms.warehouse.infrastructure.mapper.InventoryJpaMapper;
import com.g42.platform.gms.warehouse.infrastructure.mapper.WarehouseJpaMapper;
import com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.InventoryJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.WarehouseJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.WorkCategoryJpaEntityRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarehouseInternalApiImpl implements WarehouseInternalApi {
    @Autowired
    private CatalogItemJpaRepo catalogItemRepo;
    @Autowired
    private CatalogItemJpaMapper catalogItemJpaMapper;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private WorkCategoryJpaEntityRepo itemCategoryJpaRepo;
    @Autowired
    private WarehouseJpaRepo warehouseJpaRepo;
    @Autowired
    private WarehouseJpaMapper warehouseJpaMapper;
    @Autowired
    private InventoryJpaRepo inventoryJpaRepo;
    @Autowired
    private InventoryJpaMapper inventoryJpaMapper;

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

    @Override
    public Integer findCodeByCategoryCode(String categoryCode) {
        WorkCategoryJpaEntity workCategoryJpaEntity = itemCategoryJpaRepo.findByCategoryCode(categoryCode);
        if (workCategoryJpaEntity == null) {
            return null;
        }
        return workCategoryJpaEntity.getWorkCategoryId();
    }

    @Override
    public List<Warehouse> findAllById(List<Integer> workCategoryIds) {
        List<WarehouseJpa> warehouseJpas = warehouseJpaRepo.findAllById(workCategoryIds);
        return warehouseJpas.stream().map(warehouseJpaMapper::toDomain).toList();
    }

    @Override
    public CatalogItem findCatalogById(Integer getItemId) {
        CatalogItemJpa catalogItemJpa = catalogItemRepo.findById(getItemId).orElse(null);
        return catalogItemJpaMapper.toDomain(catalogItemJpa);
    }

    @Override
    public Inventory findInventoryByWarehouseIdAndItemIds(Integer warehouseId, Integer itemId) {
        return inventoryJpaMapper.toDomain(inventoryJpaRepo.findByWarehouseIdAndItemId(warehouseId,itemId).orElse(null));
    }

    @Override
    public Inventory findItemAvailableInOtherWarehouse(Integer itemId, int i) {
//        InventoryJpa inventoryJpa = inventoryJpaRepo.findByItemIdThatAvailable(itemId);
        return null;
    }
}
