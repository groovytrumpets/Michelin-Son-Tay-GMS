package com.g42.platform.gms.warehouse.app.service.pricing;

import com.g42.platform.gms.warehouse.api.dto.request.UpsertFallbackPricingRequest;
import com.g42.platform.gms.warehouse.api.dto.response.FallbackPricingResponse;
import com.g42.platform.gms.warehouse.infrastructure.entity.FallbackPricingConfigJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.FallbackPricingConfigJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FallbackPricingConfigService {

    private final FallbackPricingConfigJpaRepo fallbackRepo;

    @Transactional(readOnly = true)
    public Page<FallbackPricingResponse> search(Boolean isActive, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<FallbackPricingConfigJpa> jpaPage = fallbackRepo.search(isActive, search, pageable);

        List<FallbackPricingResponse> content = jpaPage.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(content, pageable, jpaPage.getTotalElements());
    }

    @Transactional
    public FallbackPricingResponse create(UpsertFallbackPricingRequest request) {
        // Nếu có itemType, kiểm tra xem đã có quy tắc active nào trùng itemType chưa
        if (request.getItemType() != null) {
            fallbackRepo.findFirstByItemTypeAndIsActiveTrue(request.getItemType())
                    .ifPresent(existing -> {
                        existing.setIsActive(false);
                        fallbackRepo.save(existing);
                    });
        } else {
            // Kiểm tra quy tắc mặc định chung
            fallbackRepo.findFirstByItemTypeIsNullAndIsActiveTrue()
                    .ifPresent(existing -> {
                        existing.setIsActive(false);
                        fallbackRepo.save(existing);
                    });
        }

        FallbackPricingConfigJpa config = new FallbackPricingConfigJpa();
        config.setName(request.getName());
        config.setItemType(request.getItemType());
        config.setMarkupMultiplier(request.getMarkupMultiplier());
        config.setMarkupMultiplierWholesale(resolveWholesaleMultiplier(request));
        config.setDescription(request.getDescription());
        config.setIsActive(true);
        config.setCreatedAt(Instant.now());

        return toResponse(fallbackRepo.save(config));
    }

    @Transactional
    public FallbackPricingResponse update(Integer id, UpsertFallbackPricingRequest request) {
        FallbackPricingConfigJpa config = fallbackRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình id=" + id));

        // Kiểm tra trùng lặp nếu đổi itemType hoặc tái kích hoạt
        if (config.getIsActive()) {
            if (request.getItemType() != config.getItemType()) {
                if (request.getItemType() != null) {
                    fallbackRepo.findFirstByItemTypeAndIsActiveTrue(request.getItemType())
                            .filter(existing -> !existing.getId().equals(id))
                            .ifPresent(existing -> {
                                existing.setIsActive(false);
                                fallbackRepo.save(existing);
                            });
                } else {
                    fallbackRepo.findFirstByItemTypeIsNullAndIsActiveTrue()
                            .filter(existing -> !existing.getId().equals(id))
                            .ifPresent(existing -> {
                                existing.setIsActive(false);
                                fallbackRepo.save(existing);
                            });
                }
            }
        }

        config.setName(request.getName());
        config.setItemType(request.getItemType());
        config.setMarkupMultiplier(request.getMarkupMultiplier());
        config.setMarkupMultiplierWholesale(resolveWholesaleMultiplier(request));
        config.setDescription(request.getDescription());

        return toResponse(fallbackRepo.save(config));
    }

    @Transactional
    public void deactivate(Integer id) {
        FallbackPricingConfigJpa config = fallbackRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình id=" + id));
        config.setIsActive(false);
        fallbackRepo.save(config);
    }

    @Transactional
    public void activate(Integer id) {
        FallbackPricingConfigJpa config = fallbackRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy cấu hình id=" + id));
        
        // Deactivate existing active rule for this item type
        if (config.getItemType() != null) {
            fallbackRepo.findFirstByItemTypeAndIsActiveTrue(config.getItemType())
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        existing.setIsActive(false);
                        fallbackRepo.save(existing);
                    });
        } else {
            fallbackRepo.findFirstByItemTypeIsNullAndIsActiveTrue()
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        existing.setIsActive(false);
                        fallbackRepo.save(existing);
                    });
        }

        config.setIsActive(true);
        fallbackRepo.save(config);
    }

    /**
     * Cấu hình fallback chỉ dùng một hệ số markup chung.
     * Cột markup_multiplier_wholesale vẫn được ghi cùng giá trị để các luồng cũ
     * đọc cột này (ví dụ định giá lúc nhập kho) không bị lệch giá.
     */
    private BigDecimal resolveWholesaleMultiplier(UpsertFallbackPricingRequest request) {
        return request.getMarkupMultiplierWholesale() != null
                ? request.getMarkupMultiplierWholesale()
                : request.getMarkupMultiplier();
    }

    private FallbackPricingResponse toResponse(FallbackPricingConfigJpa config) {
        FallbackPricingResponse res = new FallbackPricingResponse();
        res.setId(config.getId());
        res.setName(config.getName());
        res.setItemType(config.getItemType());
        res.setMarkupMultiplier(config.getMarkupMultiplier());
        res.setMarkupMultiplierWholesale(config.getMarkupMultiplierWholesale());
        res.setDescription(config.getDescription());
        res.setIsActive(config.getIsActive());
        res.setCreatedAt(config.getCreatedAt());
        return res;
    }
}
