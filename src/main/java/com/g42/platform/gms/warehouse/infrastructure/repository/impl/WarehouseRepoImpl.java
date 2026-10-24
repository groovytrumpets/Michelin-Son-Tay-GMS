package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.api.dto.WarehouseDetailDto;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import com.g42.platform.gms.warehouse.domain.entity.WarehousePricing;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseDetailProjection;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.g42.platform.gms.common.enums.WarehouseTypeEnum;

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
            (int page, int size, CatalogItemType itemType, Boolean isActive, String search, Integer brandId, Integer productLineId, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String sortBy, String vehicleBrand, String vehicleModel, Collection<Integer> warehouseIds, Collection<Integer> categoryIds)
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
        specification =specification.and(CatalogItemSpecification.filterCatalog(itemType,isActive,brandId,productLineId,categoryId,minPrice,maxPrice,vehicleBrand,vehicleModel));

        if (search != null && !search.trim().isEmpty()) {
            String searchLower = "%" + search.toLowerCase().trim() + "%";
            specification = specification.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("itemName")), searchLower),
                            cb.like(cb.lower(root.get("sku")), searchLower),
                            cb.like(cb.lower(root.get("partNumber")), searchLower),
                            cb.like(cb.lower(root.get("barcode")), searchLower),
                            cb.like(cb.lower(root.get("compatibleCars")), searchLower),
                            // Precomputed blob (name/sku/codes/brand/category/product line/specs) — catches
                            // matches that the individual-field LIKEs above miss, e.g. brand or category name.
                            cb.like(root.get("searchKey"), searchLower)
                    )
            );
        }
        // Lọc kho ở server: trước đây FE tải 500 mục mới nhất rồi lọc kho phía client, nên
        // hàng cũ hơn của kho đó bị rớt hẳn khỏi danh sách.
        if (warehouseIds != null && !warehouseIds.isEmpty()) {
            specification = specification.and((root, query, cb) -> {
                Subquery<Integer> sub = query.subquery(Integer.class);
                Root<InventoryJpa> inv = sub.from(InventoryJpa.class);
                sub.select(inv.get("itemId")).where(
                        cb.equal(inv.get("itemId"), root.get("itemId")),
                        inv.get("warehouseId").in(warehouseIds));
                return cb.exists(sub);
            });
        }
        // Chọn nhiều danh mục cùng lúc: mục thuộc BẤT KỲ danh mục nào trong danh sách
        if (categoryIds != null && !categoryIds.isEmpty()) {
            specification = specification.and((root, query, cb) -> root.get("itemCategoryId").in(categoryIds));
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
    public List<Warehouse> findAllWarehouses() {
        return warehouseJpaRepo.findAll().stream().map(warehouseJpaMapper::toDomain).toList();
    }

    @Override
    public Optional<Warehouse> findById(Integer warehouseId) {
        return warehouseJpaRepo.findById(warehouseId).map(warehouseJpaMapper::toDomain);
    }

    @Override
    public Optional<Warehouse> findByParentAndType(Integer parentWarehouseId, WarehouseTypeEnum warehouseType) {
        return warehouseJpaRepo
                .findByParentWarehouseIdAndWarehouseType(parentWarehouseId, warehouseType)
                .map(warehouseJpaMapper::toDomain);
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseJpa jpa =
                new com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseJpa();
        // Thiếu id thì JPA INSERT dòng mới -> trùng warehouse_code (UNIQUE), nên sửa/bật/tắt kho từng luôn lỗi 500
        jpa.setWarehouseId(warehouse.getWarehouseId());
        jpa.setWarehouseCode(warehouse.getWarehouseCode());
        jpa.setWarehouseName(warehouse.getWarehouseName());
        jpa.setWarehouseType(warehouse.getWarehouseType());
        jpa.setParentWarehouseId(warehouse.getParentWarehouseId());
        jpa.setAddress(warehouse.getAddress());
        jpa.setManagerStaffId(warehouse.getManagerStaffId());
        jpa.setIsActive(warehouse.getIsActive());
        jpa.setCreatedAt(warehouse.getCreatedAt());
        return warehouseJpaMapper.toDomain(warehouseJpaRepo.save(jpa));
    }
}
