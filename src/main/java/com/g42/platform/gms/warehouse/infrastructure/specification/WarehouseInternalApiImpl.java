package com.g42.platform.gms.warehouse.infrastructure.specification;

import com.g42.platform.gms.warehouse.app.service.dto.PricingResolve;
import org.apache.commons.lang3.tuple.Pair;
import com.g42.platform.gms.warehouse.api.dto.CatalogItemDto;
import com.g42.platform.gms.warehouse.api.dto.HomeCatalogItemInfoDto;
import com.g42.platform.gms.warehouse.api.dto.HomeStockLocationDto;
import com.g42.platform.gms.warehouse.api.dto.WarehouseDetailDto;
import com.g42.platform.gms.warehouse.api.internal.WarehouseInternalApi;
import com.g42.platform.gms.warehouse.app.service.inventory.InventoryService;
import com.g42.platform.gms.warehouse.app.service.pricing.PricingService;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseErrorCode;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseException;
import com.g42.platform.gms.warehouse.infrastructure.entity.*;
import com.g42.platform.gms.warehouse.infrastructure.mapper.CatalogItemJpaMapper;
import com.g42.platform.gms.warehouse.infrastructure.mapper.InventoryJpaMapper;
import com.g42.platform.gms.warehouse.infrastructure.mapper.WarehouseJpaMapper;
import com.g42.platform.gms.warehouse.infrastructure.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WarehouseInternalApiImpl implements WarehouseInternalApi {
    @Autowired
    private CatalogItemJpaRepo catalogItemRepo;
    @Autowired
    private CatalogItemJpaMapper catalogItemJpaMapper;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private ItemCategoryJpaRepo itemCategoryJpaRepo;
    @Autowired
    private WarehouseJpaRepo warehouseJpaRepo;
    @Autowired
    private WarehouseJpaMapper warehouseJpaMapper;
    @Autowired
    private InventoryJpaRepo inventoryJpaRepo;
    @Autowired
    private InventoryJpaMapper inventoryJpaMapper;
    @Autowired
    private PricingService pricingService;
    @Autowired
    private ReturnEntryItemJpaRepo returnEntryItemJpaRepo;
    @Autowired
    private ReturnEntryJpaRepo returnEntryJpaRepo;
    @Autowired
    private BrandJpaRepo brandJpaRepo;
    @Autowired
    private ProductLineJpaRepo productLineJpaRepo;

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
        catalogItemRepo.save(catalogItemJpa);
        System.out.println("DEBUG: catalogItem ID: " + catalogItemJpa.getServiceId()+" Saved wth serviceId: " + serviceSaved.getServiceId());
    }

    @Override
    public void updateInventoryEstimateAllocation(Integer itemId, Integer warehouseId, Integer quantity) {
        inventoryService.updateInventoryByEstimate(itemId,warehouseId,quantity);
    }

    @Override
    public Integer findCodeByCategoryCode(String categoryCode) {
        ItemCategoryJpa itemCategoryJpaEntity = itemCategoryJpaRepo.findByCategoryCode(categoryCode);
        if (itemCategoryJpaEntity == null) {
            return null;
        }
        return itemCategoryJpaEntity.getItemCategoryId();
    }

    @Override
    public List<Warehouse> findAllById(List<Integer> warehouseIds) {
        List<WarehouseJpa> warehouseJpas = warehouseJpaRepo.findAllById(warehouseIds);
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

    @Override
    public BigDecimal findItemPricing(Integer itemId, Integer integer, BigDecimal price) {
        return pricingService.getEffectivePrice(itemId,integer,price).getFinalPrice();
    }

    @Override
    public Pair<Integer,String> getReturnStatusByAlloId(Integer allocationId) {
        ReturnEntryItemJpa item = returnEntryItemJpaRepo.findTopByAllocationIdOrderByReturnItemIdDesc(allocationId);
        if (item == null || item.getReturnId() == null) {
            return null;
        }

        ReturnEntryJpa returnEntry = returnEntryJpaRepo.findById(item.getReturnId()).orElse(null);

        if (returnEntry != null && returnEntry.getStatus() != null) {
            // Khởi tạo cặp dữ liệu ID và Status Name
            return Pair.of(returnEntry.getReturnId(), returnEntry.getStatus().name());
        }

        return null;
    }

    @Override
    public BigDecimal findLatesFallBackPrice(Integer itemId, Integer warehouseId) {
        PricingResolve pricingResolve = pricingService.getEffectivePrice(itemId,warehouseId,null);
        if (pricingResolve == null) {
            return BigDecimal.ZERO;
        }
        if (pricingResolve.getFinalPrice() != null) {
        return  pricingResolve.getFinalPrice();
        }
            return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal findLatesFallBackPriceWholesale(Integer itemId, Integer warehouseId) {
        PricingResolve pricingResolve = pricingService.getEffectivePriceWholesale(itemId,warehouseId,null);
        if (pricingResolve == null) {
            return BigDecimal.ZERO;
        }
        if (pricingResolve.getFinalPrice() != null) {
            return pricingResolve.getFinalPrice();
        }
        return BigDecimal.ZERO;
    }

    @Override
    public Map<Integer, HomeCatalogItemInfoDto> getHomeCatalogInfoByItemIds(java.util.Set<Integer> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Map.of();
        }
        List<CatalogItemJpa> items = catalogItemRepo.findAllById(itemIds);

        java.util.Set<Integer> categoryIds = new java.util.HashSet<>();
        java.util.Set<Integer> brandIds = new java.util.HashSet<>();
        java.util.Set<Integer> lineIds = new java.util.HashSet<>();
        for (CatalogItemJpa item : items) {
            if (item.getItemCategoryId() != null) categoryIds.add(item.getItemCategoryId());
            if (item.getBrandId() != null && item.getBrandId() != 0) brandIds.add(item.getBrandId());
            if (item.getProductLineId() != null && item.getProductLineId() != 0) lineIds.add(item.getProductLineId());
        }

        Map<Integer, ItemCategoryJpa> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            for (ItemCategoryJpa category : itemCategoryJpaRepo.findAllById(categoryIds)) {
                categoryMap.put(category.getItemCategoryId(), category);
            }
        }
        Map<Integer, String> brandMap = brandJpaRepo.getBrandMapByIds(brandIds);
        Map<Integer, String> lineMap = productLineJpaRepo.findAllLinesByIds(lineIds);

        Map<Integer, Long> availableMap = new HashMap<>();
        for (InventoryJpaRepo.ItemAvailableProjection row : inventoryJpaRepo.sumAvailableByItemIds(itemIds)) {
            availableMap.put(row.getItemId(), row.getAvailableQty());
        }

        Map<Integer, HomeCatalogItemInfoDto> result = new HashMap<>();
        for (CatalogItemJpa item : items) {
            HomeCatalogItemInfoDto dto = new HomeCatalogItemInfoDto();
            dto.setItemId(item.getItemId());
            dto.setItemType(item.getItemType() != null ? item.getItemType().name() : null);
            dto.setPrice(item.getPrice());
            dto.setItemCategoryId(item.getItemCategoryId());
            ItemCategoryJpa category = item.getItemCategoryId() != null ? categoryMap.get(item.getItemCategoryId()) : null;
            if (category != null) {
                dto.setCategoryCode(category.getCategoryCode());
                dto.setCategoryName(category.getCategoryName());
            }
            dto.setBrandId(item.getBrandId());
            dto.setBrandName(item.getBrandId() != null ? brandMap.get(item.getBrandId()) : null);
            dto.setProductLineId(item.getProductLineId());
            dto.setProductLineName(item.getProductLineId() != null ? lineMap.get(item.getProductLineId()) : null);
            dto.setCompatibleCars(item.getCompatibleCars());
            Long available = availableMap.get(item.getItemId());
            dto.setAvailableQty(available != null ? Math.toIntExact(Math.max(0, available)) : 0);
            result.put(item.getItemId(), dto);
        }
        return result;
    }

    @Override
    public List<HomeStockLocationDto> getHomeStockLocations(Integer itemId) {
        if (itemId == null) {
            return List.of();
        }
        List<WarehouseDetailDto> details = warehouseJpaRepo.getListOfWarehouseDetailsByItemId(itemId);
        List<HomeStockLocationDto> result = new java.util.ArrayList<>();
        for (WarehouseDetailDto detail : details) {
            int available = (detail.getQuantity() != null ? detail.getQuantity() : 0)
                    - (detail.getReservedQuantity() != null ? detail.getReservedQuantity() : 0);
            if (available <= 0) continue;

            WarehouseJpa warehouse = warehouseJpaRepo.findById(detail.getWarehouseId()).orElse(null);
            if (warehouse == null || Boolean.FALSE.equals(warehouse.getIsActive())) continue;
            if (warehouse.getWarehouseType() == com.g42.platform.gms.common.enums.WarehouseTypeEnum.DEFECTIVE) continue;

            result.add(new HomeStockLocationDto(
                    detail.getWarehouseId(),
                    detail.getWarehouseCode(),
                    detail.getWarehouseName(),
                    warehouse.getWarehouseType() != null ? warehouse.getWarehouseType().name() : null,
                    detail.getWarehouseAddress(),
                    available
            ));
        }
        return result;
    }
}
