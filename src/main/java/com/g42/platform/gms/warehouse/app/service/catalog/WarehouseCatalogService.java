package com.g42.platform.gms.warehouse.app.service.catalog;

import com.g42.platform.gms.warehouse.api.dto.request.CreatePartRequest;
import com.g42.platform.gms.warehouse.api.dto.response.PartResponse;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.PartCatalogRepo;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseCatalogService {

    private static final AtomicInteger SKU_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PartCatalogRepo partCatalogRepo;
    private final InventoryRepo inventoryRepo;
    @Transactional(readOnly = true)
    public List<PartResponse> search(String keyword) {
        return partCatalogRepo.searchParts(keyword).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
    @Transactional
    public PartResponse createPart(CreatePartRequest request, Integer staffId) {
        String sku = resolveSku(request.getSku());

        CatalogItem item = new CatalogItem();
        item.setItemName(request.getItemName());
        item.setItemType(CatalogItemType.PART);
        item.setSku(sku);
        item.setPartNumber(request.getPartNumber());
        item.setBarcode(request.getBarcode());
        item.setUnit(request.getUnit());
        item.setDescription(request.getDescription());
        item.setMadeIn(request.getMadeIn());
        item.setWorkCategoryId(request.getWorkCategoryId() != null ? request.getWorkCategoryId() : 1);
        item.setBrandId(request.getBrandId());
        item.setProductLineId(request.getProductLineId());
        item.setIsActive(true);
        item.setWarrantyDurationMonths(0);

        CatalogItem saved = partCatalogRepo.save(item);

        // Tạo inventory record (qty=0) cho kho được chỉ định
        Inventory inv = Inventory.builder()
                .warehouseId(request.getWarehouseId())
                .itemId(saved.getItemId())
                .quantity(0)
                .reservedQuantity(0)
                .build();
        inventoryRepo.save(inv);

        return toResponse(saved);
    }

    private String resolveSku(String requestedSku) {
        if (requestedSku != null && !requestedSku.isBlank()) {
            if (partCatalogRepo.existsBySku(requestedSku)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "SKU '" + requestedSku + "' đã tồn tại");
            }
            return requestedSku;
        }
        String date = LocalDate.now().format(DATE_FMT);
        String candidate;
        do {
            candidate = String.format("PART-%s-%d", date, SKU_SEQ.incrementAndGet());
        } while (partCatalogRepo.existsBySku(candidate));
        return candidate;
    }

    private PartResponse toResponse(CatalogItem e) {
        PartResponse r = new PartResponse();
        r.setItemId(e.getItemId());
        r.setItemName(e.getItemName());
        r.setItemType(e.getItemType());
        r.setSku(e.getSku());
        r.setPartNumber(e.getPartNumber());
        r.setBarcode(e.getBarcode());
        r.setUnit(e.getUnit());
        r.setMadeIn(e.getMadeIn());
        r.setWorkCategoryId(e.getWorkCategoryId());
        r.setBrandId(e.getBrandId());
        r.setProductLineId(e.getProductLineId());
        r.setIsActive(e.getIsActive());
        return r;
    }
}
