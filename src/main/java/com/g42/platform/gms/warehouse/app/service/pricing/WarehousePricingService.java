package com.g42.platform.gms.warehouse.app.service.pricing;

import com.g42.platform.gms.warehouse.api.dto.request.UpsertPricingRequest;
import com.g42.platform.gms.warehouse.api.dto.response.PricingResponse;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import com.g42.platform.gms.warehouse.domain.entity.WarehousePricing;
import com.g42.platform.gms.warehouse.domain.repository.PartCatalogRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehousePricingRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehousePricingService {

    private final WarehousePricingRepo pricingRepo;
    private final PartCatalogRepo partCatalogRepo;
    private final WarehouseRepo warehouseRepo;
    @Transactional(readOnly = true)
    public List<PricingResponse> listByWarehouse(Integer warehouseId) {
        List<WarehousePricing> pricings = pricingRepo.findActiveByWarehouse(warehouseId);

        List<Integer> itemIds = pricings.stream().map(WarehousePricing::getItemId).collect(Collectors.toList());
        Map<Integer, String> nameMap = partCatalogRepo.findNamesByIds(itemIds);

        return pricings.stream()
                .map(p -> toResponse(p, nameMap.get(p.getItemId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PricingResponse> searchByWarehouse(Integer warehouseId,
                                                   Boolean isActive,
                                                   String search,
                                                   int page,
                                                   int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<WarehousePricing> pricingPage = pricingRepo.search(warehouseId, isActive, search, pageable);

        List<Integer> itemIds = pricingPage.getContent().stream()
                .map(WarehousePricing::getItemId)
                .collect(Collectors.toList());
        Map<Integer, String> nameMap = partCatalogRepo.findNamesByIds(itemIds);

        List<PricingResponse> content = pricingPage.getContent().stream()
                .map(p -> toResponse(p, nameMap.get(p.getItemId())))
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, pricingPage.getTotalElements());
    }

    @Transactional
    public PricingResponse upsert(UpsertPricingRequest request) {
        // Deactivate giá cũ nếu có
        pricingRepo.findActiveByWarehouseAndItem(request.getWarehouseId(), request.getItemId())
                .ifPresent(old -> {
                    old.setIsActive(false);
                    pricingRepo.save(old);
                });

        BigDecimal multiplier = request.getMarkupMultiplier() != null
                ? request.getMarkupMultiplier() : BigDecimal.ONE;
        BigDecimal sellingPrice = request.getSellingPrice() != null
                ? request.getSellingPrice()
                : request.getBasePrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);

        WarehousePricing pricing = new WarehousePricing();
        pricing.setWarehouseId(request.getWarehouseId());
        pricing.setItemId(request.getItemId());
        pricing.setBasePrice(request.getBasePrice());
        pricing.setMarkupMultiplier(multiplier);
        pricing.setSellingPrice(sellingPrice);
        pricing.setEffectiveFrom(request.getEffectiveFrom() != null ? request.getEffectiveFrom() : LocalDate.now());
        pricing.setEffectiveTo(request.getEffectiveTo());
        pricing.setIsActive(true);
        pricing.setCreatedAt(Instant.now());

        return toResponse(pricingRepo.save(pricing), null);
    }
    @Transactional
    public void deactivate(Integer pricingId) {
        WarehousePricing pricing = pricingRepo.findById(pricingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy cấu hình giá id=" + pricingId));
        pricing.setIsActive(false);
        pricingRepo.save(pricing);
    }

    private PricingResponse toResponse(WarehousePricing p, String itemName) {
        PricingResponse r = new PricingResponse();
        r.setPricingId(p.getPricingId());
        r.setWarehouseId(p.getWarehouseId());

        if (p.getWarehouseId() != null) {
            Warehouse warehouse = warehouseRepo.findById(p.getWarehouseId()).orElse(null);
            if (warehouse != null) {
                r.setWarehouseCode(warehouse.getWarehouseCode());
                r.setWarehouseName(warehouse.getWarehouseName());
            }
        }

        r.setItemId(p.getItemId());
        r.setItemName(itemName);
        r.setBasePrice(p.getBasePrice());
        r.setMarkupMultiplier(p.getMarkupMultiplier());
        r.setSellingPrice(p.getSellingPrice());
        r.setEffectiveFrom(p.getEffectiveFrom());
        r.setEffectiveTo(p.getEffectiveTo());
        r.setIsActive(p.getIsActive());
        return r;
    }
}
