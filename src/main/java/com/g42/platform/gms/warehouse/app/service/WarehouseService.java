package com.g42.platform.gms.warehouse.app.service;

import com.g42.platform.gms.warehouse.api.dto.CatalogSummaryDto;
import com.g42.platform.gms.warehouse.api.mapper.CatalogDtoMapper;
import com.g42.platform.gms.warehouse.domain.entity.Brand;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.ProductLine;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseErrorCode;
import com.g42.platform.gms.warehouse.domain.exception.WarehouseException;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
                .map(CatalogItem::getItemCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());

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
            if (catalogItem.getItemCategoryId() != null) {
                dto.setItemCategoryCode(cateMap.get(catalogItem.getItemCategoryId()));
            }
            return dto;
        });
    }
}
