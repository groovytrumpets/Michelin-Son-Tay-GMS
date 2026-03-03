package com.g42.platform.gms.catalog.application.service;

import com.g42.platform.gms.booking.customer.infrastructure.entity.CatalogItemJpaEntity;
import com.g42.platform.gms.catalog.api.dto.CatalogItemResponse;
import com.g42.platform.gms.catalog.exception.CatalogException;
import com.g42.platform.gms.catalog.repository.CatalogItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CatalogItemService {
    
    private final CatalogItemRepository catalogItemRepository;
    
    public List<CatalogItemResponse> getAllActiveItems() {
        List<CatalogItemJpaEntity> items = catalogItemRepository.findByIsActiveTrue();
        return items.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    public CatalogItemResponse getItemById(Integer itemId) {
        CatalogItemJpaEntity item = catalogItemRepository.findById(itemId)
                .orElseThrow(() -> new CatalogException("Không tìm thấy catalog item với ID: " + itemId));
        return mapToResponse(item);
    }
    
    private CatalogItemResponse mapToResponse(CatalogItemJpaEntity entity) {
        Integer estimateTime = extractEstimateTime(entity);
        
        return CatalogItemResponse.builder()
                .itemId(entity.getItemId())
                .itemName(entity.getItemName())
                .itemType(entity.getItemType())
                .estimatedPrice(entity.getEstimatedPrice())
                .estimateTime(estimateTime)
                .isActive(entity.getIsActive())
                .build();
    }
    
    private Integer extractEstimateTime(CatalogItemJpaEntity entity) {
        if (entity.getServiceService() == null) {
            return null;
        }
        return entity.getServiceService().getEstimateTime();
    }
}
