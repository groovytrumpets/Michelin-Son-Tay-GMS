package com.g42.platform.gms.warehouse.app.service.pricing;

import com.g42.platform.gms.warehouse.api.dto.CatalogSummaryDto;
import com.g42.platform.gms.warehouse.api.mapper.CatalogDtoMapper;
import com.g42.platform.gms.warehouse.app.service.entry.StockEntryService;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.WarehousePricingJpaRepo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PricingService {

    private final WarehousePricingJpaRepo warehousePricingJpaRepo;
    private final com.g42.platform.gms.catalog.application.service.CatalogItemService catalogItemService;
    private final CatalogItemRepo catalogItemRepo;
    private final CatalogDtoMapper catalogDtoMapper;
    private final StockEntryService stockEntryService;

    public PricingService(WarehousePricingJpaRepo warehousePricingJpaRepo, com.g42.platform.gms.catalog.application.service.CatalogItemService catalogItemService, CatalogItemRepo catalogItemRepo, CatalogDtoMapper catalogDtoMapper, StockEntryService stockEntryService) {
        this.warehousePricingJpaRepo = warehousePricingJpaRepo;
        this.catalogItemService = catalogItemService;
        this.catalogItemRepo = catalogItemRepo;
        this.catalogDtoMapper = catalogDtoMapper;
        this.stockEntryService = stockEntryService;
    }

    public BigDecimal getEffectivePrice(Integer itemId, Integer warehouseId, BigDecimal price) {
        //warehousePricing
        WarehousePricingJpa warehousePricing = warehousePricingJpaRepo.getWarehousePricingJpaByItemIdAndWarehouseId(itemId, warehouseId);
        if (warehousePricing!=null && warehousePricing.getSellingPrice().compareTo(BigDecimal.ZERO) > 0) {
            System.out.println("FILTER1");
            return warehousePricing.getSellingPrice();
        }

        //catalogPrice
        if (price!=null&&price.compareTo(BigDecimal.ZERO) > 0) {
            System.out.println("FILTER2");
        return price;
        }

        //2 way above null
        BigDecimal finalPrice = BigDecimal.ZERO;
        finalPrice = stockEntryService.findLatesFallBackPrice(itemId,warehouseId);
        System.out.println("FILTER3:"+finalPrice);
        return finalPrice;
    }
}
