package com.g42.platform.gms.warehouse.app.service.pricing;

import com.g42.platform.gms.warehouse.api.dto.CatalogSummaryDto;
import com.g42.platform.gms.warehouse.api.mapper.CatalogDtoMapper;
import com.g42.platform.gms.warehouse.app.service.entry.StockEntryService;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.WarehousePricingJpaRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PricingService {
    @Autowired
    private WarehousePricingJpaRepo warehousePricingJpaRepo;
    private com.g42.platform.gms.catalog.service.CatalogItemService catalogItemService;
    @Autowired
    private CatalogItemRepo catalogItemRepo;
    @Autowired
    private CatalogDtoMapper catalogDtoMapper;
    @Autowired
    private StockEntryService stockEntryService;


    public BigDecimal getEffectivePrice(Integer itemId, Integer warehouseId, BigDecimal price) {
        //warehousePricing
        WarehousePricingJpa warehousePricing = warehousePricingJpaRepo.getWarehousePricingJpaByItemIdAndWarehouseId(itemId, warehouseId);
//        System.out.println("Debug: " + itemId + ", " + warehousePricing);
        if (warehousePricing!=null && warehousePricing.getSellingPrice().compareTo(BigDecimal.ZERO) > 0) {
            System.out.println("FILTER1");
            return warehousePricing.getSellingPrice();
        }

        //catalogPrice
        CatalogSummaryDto catalogSummaryDto = null;
        if (price!=null && price.compareTo(BigDecimal.ZERO) > 0) {
            return price;
        }else {
            catalogSummaryDto = catalogDtoMapper.toSumaryDto(catalogItemRepo.getCatalogItemById(itemId));
        }
        if (catalogSummaryDto!=null&&catalogSummaryDto.getPrice()!=null
                &&catalogSummaryDto.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            System.out.println("FILTER2");
        return catalogSummaryDto.getPrice();
        }

        //2 way above null
        BigDecimal finalPrice = stockEntryService.findLatesFallBackPrice(itemId,warehouseId);
        System.out.println("FILTER3:"+finalPrice);
        if (finalPrice==null) {
            finalPrice=BigDecimal.ZERO;
        }
        return finalPrice;
    }
}
