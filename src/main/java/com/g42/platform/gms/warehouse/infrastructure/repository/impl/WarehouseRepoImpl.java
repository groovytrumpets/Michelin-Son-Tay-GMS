package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.api.dto.WarehouseDetailDto;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import com.g42.platform.gms.warehouse.domain.entity.WarehousePricing;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseDetailProjection;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.mapper.*;
import com.g42.platform.gms.warehouse.infrastructure.repository.*;
import com.g42.platform.gms.warehouse.infrastructure.specification.CatalogItemSpecification;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class WarehouseRepoImpl implements WarehouseRepo {
    @Autowired
    private WarehouseJpaMapper warehouseJpaMapper;
    @Autowired
    private WarehousePricingJpaMapper warehousePricingJpaMapper;
    @Autowired
    private InventoryJpaMapper inventoryJpaMapper;
    @Autowired
    private InventoryTransactionJpaMapper inventoryTransactionJpaMapper;
    @Autowired
    private StockTransferJpaMapper stockTransferJpaMapper;
    @Autowired
    private WarehouseJpaRepo  warehouseJpaRepo;
    @Autowired
    private WarehousePricingJpaRepo warehousePricingJpaRepo;
    @Autowired
    private InventoryJpaRepo inventoryJpaRepo;
    @Autowired
    private StockTransferJpaRepo stockTransferJpaRepo;
    @Autowired
    private InventoryTransactionJpaRepo inventoryTransactionJpaRepo;
    @Autowired
    private CatalogItemJpaRepo catalogItemJpaRepo;
    @Autowired
    private CatalogItemJpaMapper catalogItemJpaMapper;

    @Override
    public Page<CatalogItem> getListOfCatalogItems
            (int page, int size, CatalogItemType itemType, Boolean isActive, String search, Integer brandId, Integer productLineId, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String sortBy)
    {
        Sort sort = Sort.by(Sort.Direction.ASC, "itemName");

        if (sortBy != null && !sortBy.isEmpty()) {
            String[] parts = sortBy.split(",");
            String field = parts[0];
            Sort.Direction direction = (parts.length > 1 && "desc".equalsIgnoreCase(parts[1]))
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(direction, field);
        }
        Pageable  pageable = PageRequest.of(page, size, sort);
        Specification<CatalogItemJpa> specification = Specification.unrestricted();
        specification =specification.and(CatalogItemSpecification.filterCatalog(itemType,isActive,brandId,productLineId,categoryId,minPrice,maxPrice));

        if (search != null && !search.trim().isEmpty()) {
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("itemName")), "%" + search.toLowerCase() + "%")
            );
        }
            return catalogItemJpaRepo.findAll(specification, pageable)
                    .map(catalogItemJpaMapper::toDomain);
    }

    @Override
    public List<WarehouseDetailProjection> getWarehouseDetailsByItemIds(Set<Integer> itemIds) {
        return warehouseJpaRepo.getListOfWarehouseDetailsByItemIds(itemIds);
    }

    @Override
    public List<WarehouseDetailDto> getWarehouseDetailsByItemId(Integer itemId) {
        return warehouseJpaRepo.getListOfWarehouseDetailsByItemId(itemId);
    }

    @Override
    public List<Warehouse> getAllWarehouse() {
        return warehouseJpaRepo.findAllByIsActive(true).stream().map(warehouseJpaMapper::toDomain).toList();
    }

    @Override
    public Optional<Warehouse> findById(Integer warehouseId) {
        return warehouseJpaRepo.findById(warehouseId).map(warehouseJpaMapper::toDomain);
    }
}
