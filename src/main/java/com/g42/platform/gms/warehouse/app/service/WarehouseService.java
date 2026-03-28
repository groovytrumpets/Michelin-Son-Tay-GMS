package com.g42.platform.gms.warehouse.app.service;

import com.g42.platform.gms.warehouse.api.dto.CatalogSummaryDto;
import com.g42.platform.gms.warehouse.api.mapper.CatalogDtoMapper;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WarehouseService {
    @Autowired
    private WarehouseRepo warehouseRepo;
    @Autowired
    private CatalogDtoMapper catalogDtoMapper;


    public Page<CatalogSummaryDto> getListItems(int page, int size, CatalogItemType itemType, Boolean isActive, String search, Integer brandId, Integer productLineId, Integer categoryId, BigDecimal minPrice, BigDecimal maxPrice, String sortBy) {
        Page<CatalogItem> catalogItems = warehouseRepo.getListOfCatalogItems(page,size,itemType,isActive,search,brandId,productLineId,categoryId,minPrice,maxPrice,sortBy);
        return catalogItems.map(catalogDtoMapper::toSumaryDto);
    }
}
