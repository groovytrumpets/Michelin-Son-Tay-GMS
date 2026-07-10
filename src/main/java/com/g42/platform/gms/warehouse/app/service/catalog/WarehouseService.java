package com.g42.platform.gms.warehouse.app.service.catalog;

import com.g42.platform.gms.warehouse.api.dto.CatalogDetailDto;
import com.g42.platform.gms.warehouse.api.dto.CatalogSummaryDto;
import com.g42.platform.gms.warehouse.api.dto.CatalogWarehouseDto;
import com.g42.platform.gms.warehouse.api.dto.WarehouseDetailDto;
import com.g42.platform.gms.warehouse.api.dto.WarehouseDto;
import com.g42.platform.gms.warehouse.api.dto.WarehouseLotDto;
import com.g42.platform.gms.warehouse.api.dto.request.CreateWarehouseRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateWarehouseRequest;
import com.g42.platform.gms.warehouse.api.mapper.CatalogDtoMapper;
import com.g42.platform.gms.warehouse.app.service.dto.PricingResolve;
import com.g42.platform.gms.warehouse.app.service.pricing.PricingService;
import com.g42.platform.gms.warehouse.domain.entity.Brand;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.ProductLine;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import com.g42.platform.gms.warehouse.domain.entity.WarehousePricing;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseErrorCode;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseException;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseDetailProjection;
import com.g42.platform.gms.warehouse.domain.repository.WarehousePricingRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockEntryItemJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WarehouseService {
    @Autowired
    private WarehouseRepo warehouseRepo;
    @Autowired
    private CatalogItemRepo catalogItemRepo;
    @Autowired
    private CatalogItemService catalogItemService;
    @Autowired
    private CatalogDtoMapper catalogDtoMapper;
    @Autowired
    private PricingService  pricingService;
    @Autowired
    private StockEntryItemJpaRepo stockEntryItemJpaRepo;
    @Autowired
    private WarehousePricingRepo warehousePricingRepo;

    // ─── CRUD Warehouse ───────────────────────────────────────────────────────

    /** Tạo kho mới (MASTER, BRANCH hoặc DEFECTIVE). */
    @Transactional
    public WarehouseDto createWarehouse(CreateWarehouseRequest req) {
        // validate code unique
        boolean codeExists = warehouseRepo.getAllWarehouse().stream()
                .anyMatch(w -> req.getWarehouseCode().equalsIgnoreCase(w.getWarehouseCode()));
        if (codeExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Mã kho '" + req.getWarehouseCode() + "' đã tồn tại");
        }

        // BRANCH / DEFECTIVE phải có kho cha
        if (req.getWarehouseType() != com.g42.platform.gms.common.enums.WarehouseTypeEnum.MASTER) {
            if (req.getParentWarehouseId() == null) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Kho loại " + req.getWarehouseType() + " phải có kho cha (parentWarehouseId)");
            }
            warehouseRepo.findById(req.getParentWarehouseId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Không tìm thấy kho cha id=" + req.getParentWarehouseId()));
        }

        // DEFECTIVE: không tạo trùng cho cùng 1 kho cha
        if (req.getWarehouseType() == com.g42.platform.gms.common.enums.WarehouseTypeEnum.DEFECTIVE) {
            warehouseRepo.findByParentAndType(req.getParentWarehouseId(),
                            com.g42.platform.gms.common.enums.WarehouseTypeEnum.DEFECTIVE)
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Kho lỗi (DEFECTIVE) đã tồn tại cho kho cha id=" + req.getParentWarehouseId());
                    });
        }

        Warehouse w = new Warehouse();
        w.setWarehouseCode(req.getWarehouseCode().trim().toUpperCase());
        w.setWarehouseName(req.getWarehouseName().trim());
        w.setWarehouseType(req.getWarehouseType());
        w.setParentWarehouseId(req.getParentWarehouseId());
        w.setAddress(req.getAddress());
        w.setManagerStaffId(req.getManagerStaffId());
        w.setIsActive(true);
        w.setCreatedAt(java.time.Instant.now());

        return toDto(warehouseRepo.save(w));
    }

    /** Cập nhật tên, địa chỉ, manager của kho. */
    @Transactional
    public WarehouseDto updateWarehouse(Integer warehouseId, UpdateWarehouseRequest req) {
        Warehouse w = warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy kho id=" + warehouseId));

        if (req.getWarehouseName() != null && !req.getWarehouseName().isBlank()) {
            w.setWarehouseName(req.getWarehouseName().trim());
        }
        if (req.getAddress() != null) {
            w.setAddress(req.getAddress());
        }
        if (req.getManagerStaffId() != null) {
            w.setManagerStaffId(req.getManagerStaffId());
        }

        return toDto(warehouseRepo.save(w));
    }

    /** Bật/tắt trạng thái kho. */
    @Transactional
    public WarehouseDto setWarehouseActive(Integer warehouseId, boolean active) {
        Warehouse w = warehouseRepo.findById(warehouseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy kho id=" + warehouseId));
        w.setIsActive(active);
        return toDto(warehouseRepo.save(w));
    }

    /** Lấy chi tiết 1 kho. */
    public WarehouseDto getWarehouse(Integer warehouseId) {
        return warehouseRepo.findById(warehouseId)
                .map(this::toDto)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy kho id=" + warehouseId));
    }

    /** Danh sách tất cả kho (có thể lọc theo isActive). */
    public java.util.List<WarehouseDto> listWarehouses(Boolean isActive) {
        return warehouseRepo.getAllWarehouse().stream()
                .filter(w -> isActive == null || isActive.equals(w.getIsActive()))
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    private WarehouseDto toDto(Warehouse w) {
        return new WarehouseDto(
                w.getWarehouseId(),
                w.getWarehouseCode(),
                w.getWarehouseName(),
                w.getWarehouseType(),
                w.getParentWarehouseId(),
                w.getAddress(),
                w.getManagerStaffId(),
                w.getIsActive(),
                w.getCreatedAt()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo kho hàng lỗi (DEFECTIVE) cho một chi nhánh.
     *
     * @param branchWarehouseId - ID kho chi nhánh chính
     */
    @Transactional
    public void createDefectiveWarehouse(Integer branchWarehouseId) {
        com.g42.platform.gms.warehouse.domain.entity.Warehouse parentWarehouse = warehouseRepo.findById(branchWarehouseId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND,
                        "Không tìm thấy kho với ID: " + branchWarehouseId
                ));

        Optional<com.g42.platform.gms.warehouse.domain.entity.Warehouse> existing =
                warehouseRepo.findByParentAndType(branchWarehouseId, com.g42.platform.gms.common.enums.WarehouseTypeEnum.DEFECTIVE);

        if (existing.isPresent()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "Kho DEFECTIVE đã tồn tại cho warehouse ID: " + branchWarehouseId +
                            " (Kho lỗi ID: " + existing.get().getWarehouseId() + ")"
            );
        }

        com.g42.platform.gms.warehouse.domain.entity.Warehouse defectiveWarehouse =
                new com.g42.platform.gms.warehouse.domain.entity.Warehouse();

        defectiveWarehouse.setWarehouseCode(parentWarehouse.getWarehouseCode() + "-LOI");
        defectiveWarehouse.setWarehouseName("Kho hàng lỗi - " + parentWarehouse.getWarehouseName());
        defectiveWarehouse.setWarehouseType(com.g42.platform.gms.common.enums.WarehouseTypeEnum.DEFECTIVE);
        defectiveWarehouse.setParentWarehouseId(branchWarehouseId);
        defectiveWarehouse.setAddress(parentWarehouse.getAddress());
        defectiveWarehouse.setManagerStaffId(parentWarehouse.getManagerStaffId());
        defectiveWarehouse.setIsActive(true);
        defectiveWarehouse.setCreatedAt(java.time.Instant.now());

        warehouseRepo   .save(defectiveWarehouse);
    }


    public Page<CatalogSummaryDto> getListItems(int page, int size, CatalogItemType itemType, Boolean isActive, String search, Integer brand, Integer productLine, String categoryCode, BigDecimal minPrice, BigDecimal maxPrice, String sortBy, String vehicleBrand, String vehicleModel) {
        Integer resolvedCategoryId = null;
        if (categoryCode != null) {
            resolvedCategoryId = catalogItemService.findCodeByCategoryCode(categoryCode);
        }
        Page<CatalogItem> catalogItems = warehouseRepo.getListOfCatalogItems(page,size,itemType,isActive,search,brand,productLine,resolvedCategoryId,minPrice,maxPrice,sortBy,vehicleBrand,vehicleModel);
        //get all ids of brands and lineProduct to query find string
        Set<Integer> brandIds = catalogItems.stream()
                .map(CatalogItem::getBrandId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Integer> lineIds = catalogItems.stream()
                .map(CatalogItem::getProductLineId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Integer> categoryIds = catalogItems.stream()
                .map(CatalogItem::getWorkCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Integer, String> brandMap = catalogItemRepo.getAllBrandByIds(brandIds);

        Map<Integer, String> lineMap = catalogItemRepo.findAllLinesByIds(lineIds);

        Map<Integer, String> cateMap = catalogItemRepo.findAllCatesByIds(categoryIds);
        return catalogItems.map(catalogItem -> {
            CatalogSummaryDto dto = catalogDtoMapper.toSumaryDto(catalogItem);
            if (catalogItem.getBrandId() != null) {
                dto.setBrand(brandMap.get(catalogItem.getBrandId()));
            }
            if (catalogItem.getProductLineId() != null) {
                dto.setProductLine(lineMap.get(catalogItem.getProductLineId()));
            }
            if (catalogItem.getWorkCategoryId() != null) {
                dto.setItemCategoryCode(cateMap.get(catalogItem.getWorkCategoryId()));
            }
            return dto;
        });
    }
    public Page<CatalogWarehouseDto> getListItemsDetail(int page, int size, CatalogItemType itemType, Boolean isActive, String search, Integer brand, Integer productLine, String categoryCode, BigDecimal minPrice, BigDecimal maxPrice, String sortBy, String vehicleBrand, String vehicleModel) {
        Integer resolvedCategoryId = null;
        if (categoryCode != null) {
            resolvedCategoryId = catalogItemService.findCodeByCategoryCode(categoryCode);
        }
        Page<CatalogItem> catalogItems = warehouseRepo.getListOfCatalogItems(page,size,itemType,isActive,search,brand,productLine,resolvedCategoryId,minPrice,maxPrice,sortBy,vehicleBrand,vehicleModel);
        //get all ids of brands and lineProduct to query find string
        Set<Integer> brandIds = catalogItems.stream()
                .map(CatalogItem::getBrandId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Integer> lineIds = catalogItems.stream()
                .map(CatalogItem::getProductLineId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Integer> categoryIds = catalogItems.stream()
                .map(CatalogItem::getWorkCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Integer> itemIds = catalogItems.stream()
                .map(CatalogItem::getItemId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Integer, String> brandMap = catalogItemRepo.getAllBrandByIds(brandIds);

        Map<Integer, String> lineMap = catalogItemRepo.findAllLinesByIds(lineIds);

        Map<Integer, String> cateMap = catalogItemRepo.findAllCatesByIds(categoryIds);

        Map<Integer, List<WarehouseDetailDto>> itemWarehouseMap;

        if (!itemIds.isEmpty()) {
            // Query 1 lần lấy toàn bộ thông tin kho của các item trên trang hiện tại
            List<WarehouseDetailProjection> warehouseProjections = warehouseRepo.getWarehouseDetailsByItemIds(itemIds);

            itemWarehouseMap = warehouseProjections.stream()
                    .collect(Collectors.groupingBy(
                            WarehouseDetailProjection::getItemId, // Nhóm theo itemId
                            Collectors.mapping(prj -> new WarehouseDetailDto(
                                    prj.getWarehouseId(),
                                    prj.getWarehouseCode(),
                                    prj.getWarehouseName(),
                                    prj.getWarehouseAddress(),
                                    prj.getItemId(),
                                    prj.getSellingPrice(),
                                    prj.getQuantity(),
                                    prj.getReservedQuantity(),
                                    prj.getMinStockLevel(),
                                    prj.getMaxStockLevel(),
                                    prj.getAvailableStockLevel(),
                                    prj.getNotify()
                            ), Collectors.toList()) // Map Projection thành DTO và gom thành List
                    ));
        } else {
            itemWarehouseMap = new HashMap<>();
        }
        return catalogItems.map(catalogItem -> {
            CatalogWarehouseDto dto = catalogDtoMapper.toSumaryWarehouseDto(catalogItem);
            if (catalogItem.getBrandId() != null) {
                dto.setBrand(brandMap.get(catalogItem.getBrandId()));
            }
            if (catalogItem.getProductLineId() != null) {
                dto.setProductLine(lineMap.get(catalogItem.getProductLineId()));
            }
            if (catalogItem.getWorkCategoryId() != null) {
                dto.setItemCategoryCode(cateMap.get(catalogItem.getWorkCategoryId()));
            }
            List<WarehouseDetailDto> details = itemWarehouseMap.getOrDefault(catalogItem.getItemId(), new ArrayList<>());
            dto.setWarehouseDetails(details);
            for (WarehouseDetailDto detail : details) {
                // Gọi PricingService, truyền sẵn Lớp 1 (detail.getSellingPrice())
                // và Lớp 2 (catalogItem.getPrice()) vào để tối ưu hiệu năng.
                PricingResolve pricingResolve = pricingService.getEffectivePrice(
                        catalogItem.getItemId(),
                        detail.getWarehouseId(),
                        catalogItem.getPrice()
                );
                // Set giá trị cuối cùng vào DTO (Bạn nhớ thêm thuộc tính effectivePrice vào WarehouseDetailDto nhé)
                detail.setSellingPrice(pricingResolve.getFinalPrice());
                detail.setNotify(pricingResolve.getNotify());

                // Query các lô hàng cho kho và vật tư này
                List<WarehouseLotDto> lots = stockEntryItemJpaRepo.findWarehouseLots(detail.getWarehouseId(), catalogItem.getItemId());

                // Lấy cấu hình giá bán cố định nếu có
                BigDecimal warehouseSellingPrice = warehousePricingRepo.findActiveByWarehouseAndItem(detail.getWarehouseId(), catalogItem.getItemId())
                        .map(WarehousePricing::getSellingPrice)
                        .orElse(null);
                BigDecimal warehouseSellingPriceWholesale = warehousePricingRepo.findActiveByWarehouseAndItem(detail.getWarehouseId(), catalogItem.getItemId())
                        .map(WarehousePricing::getSellingPriceWholesale)
                        .orElse(null);

                for (WarehouseLotDto lot : lots) {
                    BigDecimal lotSellingPrice;
                    if (warehouseSellingPrice != null && warehouseSellingPrice.compareTo(BigDecimal.ZERO) > 0) {
                        lotSellingPrice = warehouseSellingPrice;
                    } else if (lot.getImportPrice() != null && lot.getImportPrice().compareTo(BigDecimal.ZERO) > 0) {
                        lotSellingPrice = lot.getImportPrice()
                                .multiply(lot.getMarkupMultiplier() != null ? lot.getMarkupMultiplier() : BigDecimal.ONE)
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                    } else {
                        lotSellingPrice = catalogItem.getPrice() != null ? catalogItem.getPrice() : BigDecimal.ZERO;
                    }
                    lot.setSellingPrice(lotSellingPrice);

                    BigDecimal lotSellingPriceWholesale;
                    if (warehouseSellingPriceWholesale != null && warehouseSellingPriceWholesale.compareTo(BigDecimal.ZERO) > 0) {
                        lotSellingPriceWholesale = warehouseSellingPriceWholesale;
                    } else if (lot.getImportPrice() != null && lot.getImportPrice().compareTo(BigDecimal.ZERO) > 0) {
                        lotSellingPriceWholesale = lot.getImportPrice()
                                .multiply(lot.getMarkupMultiplierWholesale() != null ? lot.getMarkupMultiplierWholesale() : lot.getMarkupMultiplier() != null ? lot.getMarkupMultiplier() : BigDecimal.ONE)
                                .setScale(2, java.math.RoundingMode.HALF_UP);
                    } else {
                        lotSellingPriceWholesale = catalogItem.getPrice() != null ? catalogItem.getPrice() : BigDecimal.ZERO;
                    }
                    lot.setSellingPriceWholesale(lotSellingPriceWholesale);
                }
                detail.setLots(lots);
            }
            return dto;
        });
    }
}
