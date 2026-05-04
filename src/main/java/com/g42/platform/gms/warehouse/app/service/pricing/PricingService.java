package com.g42.platform.gms.warehouse.app.service.pricing;

import com.g42.platform.gms.warehouse.api.dto.CatalogSummaryDto;
import com.g42.platform.gms.warehouse.api.mapper.CatalogDtoMapper;
import com.g42.platform.gms.warehouse.app.service.dto.PricingResolve;
import com.g42.platform.gms.warehouse.app.service.entry.StockEntryService;
import com.g42.platform.gms.warehouse.domain.entity.WarehousePricing;
import com.g42.platform.gms.warehouse.domain.repository.CatalogItemRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehousePricingRepo;
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

    public PricingResolve getEffectivePrice(Integer itemId, Integer warehouseId, BigDecimal price) {
        PricingResolve  pricingResolve = new PricingResolve();
        // 1. Ưu tiên warehouse_pricing (manager set)
        WarehousePricing warehousePricing = warehousePricingRepo
                .findByItemIdAndWarehouseId(itemId, warehouseId).orElse(null);
        if (warehousePricing != null && warehousePricing.getSellingPrice().compareTo(BigDecimal.ZERO) > 0) {
            pricingResolve.setFinalPrice(warehousePricing.getSellingPrice());
            pricingResolve.setNotify("Đang dùng giá cài đặt sẵn");
            return  pricingResolve;
        }

        // 2. Fallback: giá nhập mới nhất từ lô
        BigDecimal finalPrice = stockEntryService.findLatesFallBackPrice(itemId, warehouseId);
        if (finalPrice != null && finalPrice.compareTo(BigDecimal.ZERO) > 0) {
            pricingResolve.setFinalPrice(finalPrice);
            pricingResolve.setNotify("Đang dùng giá nhập kho nhập mới nhất");
            return pricingResolve;
        }

        // 3. Giá từ catalog
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            pricingResolve.setFinalPrice(price);
            pricingResolve.setNotify("Đang dùng giá niêm yết của sản phẩm");
            return  pricingResolve;
        }
        CatalogSummaryDto catalogSummaryDto = catalogDtoMapper.toSumaryDto(catalogItemRepo.getCatalogItemById(itemId));
        if (catalogSummaryDto != null && catalogSummaryDto.getPrice() != null
                && catalogSummaryDto.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            pricingResolve.setFinalPrice(catalogSummaryDto.getPrice());
            pricingResolve.setNotify("Đang dùng giá niêm yết của sản phẩm");
            return pricingResolve;
        }

        pricingResolve.setFinalPrice(BigDecimal.ZERO);
        pricingResolve.setNotify("Không tìm thấy giá phù hợp trong cơ sở dữ liệu");
        return pricingResolve;
    }
}
