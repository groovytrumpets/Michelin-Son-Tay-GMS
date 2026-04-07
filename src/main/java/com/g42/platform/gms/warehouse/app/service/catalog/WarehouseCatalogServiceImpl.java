package com.g42.platform.gms.warehouse.app.service.catalog;

import com.g42.platform.gms.warehouse.api.dto.request.CreatePartRequest;
import com.g42.platform.gms.warehouse.api.dto.response.PartResponse;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.domain.repository.PartCatalogRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
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
public class WarehouseCatalogServiceImpl implements WarehouseCatalogService {

    private static final AtomicInteger SKU_SEQ = new AtomicInteger(0);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PartCatalogRepo partCatalogRepo;

    @Override
    @Transactional(readOnly = true)
    public List<PartResponse> search(String keyword) {
        Specification<CatalogItemJpa> spec = (root, query, cb) -> {
            String like = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("itemName")), like),
                    cb.like(cb.lower(cb.coalesce(root.get("sku"), "")), like),
                    cb.like(cb.lower(cb.coalesce(root.get("partNumber"), "")), like),
                    cb.like(cb.lower(cb.coalesce(root.get("barcode"), "")), like)
            );
        };
        return partCatalogRepo.findAll(spec).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PartResponse createPart(CreatePartRequest request, Integer staffId) {
        String sku = resolveSku(request.getSku());

        CatalogItemJpa item = new CatalogItemJpa();
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

        return toResponse(partCatalogRepo.save(item));
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

    private PartResponse toResponse(CatalogItemJpa e) {
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
