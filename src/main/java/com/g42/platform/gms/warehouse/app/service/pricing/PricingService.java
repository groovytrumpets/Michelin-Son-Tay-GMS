package com.g42.platform.gms.warehouse.app.service.pricing;

import com.g42.platform.gms.warehouse.api.dto.CatalogSummaryDto;
import com.g42.platform.gms.warehouse.api.mapper.CatalogDtoMapper;
import com.g42.platform.gms.warehouse.app.service.entry.StockEntryService;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehousePricingRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final WarehousePricingRepo warehousePricingRepo;
    private final com.g42.platform.gms.catalog.service.CatalogItemService catalogItemService;
    private final CatalogItemRepo catalogItemRepo;
    private final CatalogDtoMapper catalogDtoMapper;
    private final StockEntryService stockEntryService;

    public BigDecimal getEffectivePrice(Integer itemId, Integer warehouseId, BigDecimal price) {
        // 1. Ưu tiên warehouse_pricing (manager set)
        WarehousePricingJpa warehousePricing = warehousePricingRepo
                .findByItemIdAndWarehouseId(itemId, warehouseId).orElse(null);
        if (warehousePricing != null && warehousePricing.getSellingPrice().compareTo(BigDecimal.ZERO) > 0) {
            return warehousePricing.getSellingPrice();
        }

        // 2. Giá từ catalog
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            return price;
        }
        CatalogSummaryDto catalogSummaryDto = catalogDtoMapper.toSumaryDto(catalogItemRepo.getCatalogItemById(itemId));
        if (catalogSummaryDto != null && catalogSummaryDto.getPrice() != null
                && catalogSummaryDto.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return catalogSummaryDto.getPrice();
        }

        // 3. Fallback: giá nhập mới nhất từ lô
        BigDecimal finalPrice = stockEntryService.findLatesFallBackPrice(itemId, warehouseId);
        return finalPrice != null ? finalPrice : BigDecimal.ZERO;
    }
}
