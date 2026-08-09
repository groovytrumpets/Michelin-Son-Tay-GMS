package com.g42.platform.gms.estimation.app.service;

import com.g42.platform.gms.estimation.api.dto.WorkCataDto;
import com.g42.platform.gms.estimation.api.dto.request.WorkCategoryReqDto;
import com.g42.platform.gms.estimation.api.internal.TaxRuleInternalApi;
import com.g42.platform.gms.estimation.api.mapper.EstimateDtoMapper;
import com.g42.platform.gms.estimation.domain.entity.WorkCategory;
import com.g42.platform.gms.estimation.domain.repository.WorkCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Quản lý danh mục Hạng mục công việc (work_category) cho màn cấu hình hệ thống.
 *
 * Hạng mục vừa dùng để nhóm dòng báo giá, vừa là nơi khai thuế mặc định cho
 * nhóm đó, nên xóa cứng dễ làm hỏng các phiếu cũ. Vì vậy thao tác "xóa" ở đây
 * là ẩn (is_active = false), phiếu cũ vẫn tham chiếu được.
 */
@Service
@RequiredArgsConstructor
public class WorkCategoryService {

    private final WorkCategoryRepository workCategoryRepo;
    private final EstimateDtoMapper estimateDtoMapper;
    private final TaxRuleInternalApi taxRuleInternalApi;

    /** Danh sách đầy đủ cho màn cấu hình, gồm cả hạng mục đã ẩn. */
    public List<WorkCataDto> getAllForConfig() {
        return workCategoryRepo.findAllIncludingInactive().stream()
                .sorted((a, b) -> {
                    int orderA = a.getDisplayOrder() == null ? Integer.MAX_VALUE : a.getDisplayOrder();
                    int orderB = b.getDisplayOrder() == null ? Integer.MAX_VALUE : b.getDisplayOrder();
                    if (orderA != orderB) return Integer.compare(orderA, orderB);
                    return String.valueOf(a.getCategoryName()).compareToIgnoreCase(String.valueOf(b.getCategoryName()));
                })
                .map(estimateDtoMapper::toWorkCateDto)
                .toList();
    }

    @Transactional
    public WorkCataDto create(WorkCategoryReqDto request) {
        String name = request.getCategoryName() == null ? "" : request.getCategoryName().trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Tên hạng mục không được để trống.");
        }
        if (workCategoryRepo.findByCategoryName(name) != null) {
            throw new IllegalArgumentException("Hạng mục \"" + name + "\" đã tồn tại.");
        }

        WorkCategory category = new WorkCategory();
        category.setCategoryName(name);
        category.setCategoryCode(resolveCode(request.getCategoryCode(), name));
        category.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
        category.setIsActive(request.getIsActive() == null || request.getIsActive());
        category.setTaxRuleId(resolveTaxRuleId(request.getTaxRuleId()));
        category.setDisplayOrder(
                request.getDisplayOrder() != null
                        ? request.getDisplayOrder()
                        : workCategoryRepo.findMaxDisplayOrder() + 1);

        return estimateDtoMapper.toWorkCateDto(workCategoryRepo.save(category));
    }

    @Transactional
    public WorkCataDto update(Integer categoryId, WorkCategoryReqDto request) {
        WorkCategory category = workCategoryRepo.findById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Không tìm thấy hạng mục #" + categoryId);
        }

        if (request.getCategoryName() != null && !request.getCategoryName().isBlank()) {
            String name = request.getCategoryName().trim();
            WorkCategory duplicated = workCategoryRepo.findByCategoryName(name);
            if (duplicated != null && !duplicated.getId().equals(categoryId)) {
                throw new IllegalArgumentException("Hạng mục \"" + name + "\" đã tồn tại.");
            }
            category.setCategoryName(name);
        }
        if (request.getCategoryCode() != null && !request.getCategoryCode().isBlank()) {
            category.setCategoryCode(request.getCategoryCode().trim().toUpperCase().replace(" ", "_"));
        }
        if (request.getDisplayOrder() != null) category.setDisplayOrder(request.getDisplayOrder());
        if (request.getTaxRuleId() != null) category.setTaxRuleId(request.getTaxRuleId());
        if (request.getIsDefault() != null) category.setIsDefault(request.getIsDefault());
        if (request.getIsActive() != null) category.setIsActive(request.getIsActive());

        return estimateDtoMapper.toWorkCateDto(workCategoryRepo.save(category));
    }

    /** Ẩn hạng mục thay vì xóa, để không phá vỡ các phiếu báo giá đang tham chiếu. */
    @Transactional
    public WorkCataDto deactivate(Integer categoryId) {
        WorkCategory category = workCategoryRepo.findById(categoryId);
        if (category == null) {
            throw new IllegalArgumentException("Không tìm thấy hạng mục #" + categoryId);
        }
        category.setIsActive(false);
        return estimateDtoMapper.toWorkCateDto(workCategoryRepo.save(category));
    }

    private String resolveCode(String rawCode, String name) {
        String code = rawCode == null ? "" : rawCode.trim();
        if (!code.isEmpty()) return code.toUpperCase().replace(" ", "_");
        return name.toUpperCase().replace(" ", "_");
    }

    private Integer resolveTaxRuleId(Integer requested) {
        if (requested != null) return requested;
        // Không chọn thuế thì gắn quy tắc thuế 0% dùng chung, cột này không cho null
        Integer freeTaxId = taxRuleInternalApi.getTaxCodeFreeId("FREE");
        if (freeTaxId == null || freeTaxId == -1) freeTaxId = taxRuleInternalApi.createNewFreeTax();
        return freeTaxId;
    }
}
