package com.g42.platform.gms.warehouse.app.service.catalog;

import com.g42.platform.gms.warehouse.api.dto.CatalogDetailDto;
import com.g42.platform.gms.warehouse.api.dto.CatalogSummaryDto;
import com.g42.platform.gms.warehouse.api.dto.CatalogWarehouseDto;
import com.g42.platform.gms.warehouse.api.dto.WarehouseDetailDto;
import com.g42.platform.gms.warehouse.api.mapper.CatalogDtoMapper;
import com.g42.platform.gms.warehouse.app.service.pricing.PricingService;
import com.g42.platform.gms.warehouse.domain.entity.Brand;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.ProductLine;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseErrorCode;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseException;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseDetailProjection;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

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


    public Page<CatalogSummaryDto> getListItems(int page, int size, CatalogItemType itemType, Boolean isActive, String search, Integer brand, Integer productLine, String categoryCode, BigDecimal minPrice, BigDecimal maxPrice, String sortBy) {
        Integer resolvedCategoryId = null;
        if (categoryCode != null) {
            resolvedCategoryId = catalogItemService.findCodeByCategoryCode(categoryCode);
        }
        Page<CatalogItem> catalogItems = warehouseRepo.getListOfCatalogItems(page,size,itemType,isActive,search,brand,productLine,resolvedCategoryId,minPrice,maxPrice,sortBy);
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
    public Page<CatalogWarehouseDto> getListItemsDetail(int page, int size, CatalogItemType itemType, Boolean isActive, String search, Integer brand, Integer productLine, String categoryCode, BigDecimal minPrice, BigDecimal maxPrice, String sortBy) {
        Integer resolvedCategoryId = null;
        if (categoryCode != null) {
            resolvedCategoryId = catalogItemService.findCodeByCategoryCode(categoryCode);
        }
        Page<CatalogItem> catalogItems = warehouseRepo.getListOfCatalogItems(page,size,itemType,isActive,search,brand,productLine,resolvedCategoryId,minPrice,maxPrice,sortBy);
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
                                    prj.getMaxStockLevel()
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
                BigDecimal effectivePrice = pricingService.getEffectivePrice(
                        catalogItem.getItemId(),
                        detail.getWarehouseId(),
                        catalogItem.getPrice()
                );

                // Set giá trị cuối cùng vào DTO (Bạn nhớ thêm thuộc tính effectivePrice vào WarehouseDetailDto nhé)
                detail.setSellingPrice(effectivePrice);
            }
            return dto;
        });
    }
}
