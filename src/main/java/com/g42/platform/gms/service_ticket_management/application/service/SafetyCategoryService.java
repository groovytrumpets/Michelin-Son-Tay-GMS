package com.g42.platform.gms.service_ticket_management.application.service;

import com.g42.platform.gms.service_ticket_management.api.dto.safety.CreateWorkCategoryRequest;
import com.g42.platform.gms.service_ticket_management.api.dto.safety.WorkCategoryResponse;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyWorkCategoryJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.SafetyInspectionItemRepository;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.WorkCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;

/**
 * Cấu hình danh sách đầu mục kiểm tra an toàn (bảng work_category).
 *
 * Trước đây danh sách này không có màn cấu hình riêng: nó được suy ra bằng cách lọc
 * work_category theo is_default, mà bảng đó lại đang chứa lẫn danh mục phụ tùng nên
 * phiếu kiểm tra hiện ra cả "Lốp ô tô", "Gạt mưa". Sau khi tách bảng ở changeset 014,
 * đây là nơi duy nhất quản lý danh sách đầu mục kiểm tra.
 *
 * Đầu mục đã xuất hiện trên phiếu kiểm tra cũ thì chỉ ẩn chứ không xóa, vì
 * safety_inspection_item tham chiếu tới nó với ON DELETE RESTRICT.
 */
@Service
@RequiredArgsConstructor
public class SafetyCategoryService {

    private final WorkCategoryRepository workCategoryRepository;
    private final SafetyInspectionItemRepository safetyInspectionItemRepository;

    /** Toàn bộ đầu mục, gồm cả đã ẩn, cho màn cấu hình. */
    public List<WorkCategoryResponse> getAllForConfig() {
        return workCategoryRepository.findAll().stream()
                .sorted(Comparator.comparingInt(
                        (SafetyWorkCategoryJpa c) -> c.getDisplayOrder() == null ? Integer.MAX_VALUE : c.getDisplayOrder()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public WorkCategoryResponse create(CreateWorkCategoryRequest request) {
        String name = trimmed(request.getCategoryName());
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Tên hạng mục không được để trống.");
        }
        if (workCategoryRepository.findFirstByCategoryNameIgnoreCase(name) != null) {
            throw new IllegalArgumentException("Hạng mục \"" + name + "\" đã tồn tại.");
        }

        SafetyWorkCategoryJpa entity = new SafetyWorkCategoryJpa();
        entity.setCategoryName(name);
        entity.setCategoryCode(uniqueCode(resolveCode(request.getCategoryCode(), name)));
        entity.setDisplayOrder(request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : workCategoryRepository.findMaxDisplayOrder() + 1);
        entity.setIsActive(true);

        return toResponse(workCategoryRepository.save(entity));
    }

    @Transactional
    public WorkCategoryResponse update(Integer categoryId, CreateWorkCategoryRequest request) {
        SafetyWorkCategoryJpa entity = workCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hạng mục #" + categoryId));

        String name = trimmed(request.getCategoryName());
        if (!name.isEmpty()) {
            SafetyWorkCategoryJpa duplicated = workCategoryRepository.findFirstByCategoryNameIgnoreCase(name);
            if (duplicated != null && !duplicated.getId().equals(categoryId)) {
                throw new IllegalArgumentException("Hạng mục \"" + name + "\" đã tồn tại.");
            }
            entity.setCategoryName(name);
        }
        String code = trimmed(request.getCategoryCode());
        if (!code.isEmpty()) entity.setCategoryCode(normalizeCode(code));
        if (request.getDisplayOrder() != null) entity.setDisplayOrder(request.getDisplayOrder());
        if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());

        return toResponse(workCategoryRepository.save(entity));
    }

    /**
     * Đầu mục đã dùng ở phiếu kiểm tra nào đó thì chỉ ẩn; chưa dùng bao giờ thì xóa hẳn
     * để danh sách không phình ra vì những lần thêm nhầm.
     */
    @Transactional
    public void deactivate(Integer categoryId) {
        SafetyWorkCategoryJpa entity = workCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hạng mục #" + categoryId));

        if (safetyInspectionItemRepository.existsByWorkCategoryId(categoryId)) {
            entity.setIsActive(false);
            workCategoryRepository.save(entity);
        } else {
            workCategoryRepository.deleteById(categoryId);
        }
    }

    private WorkCategoryResponse toResponse(SafetyWorkCategoryJpa entity) {
        WorkCategoryResponse response = new WorkCategoryResponse();
        response.setId(entity.getId());
        response.setCategoryCode(entity.getCategoryCode());
        response.setCategoryName(entity.getCategoryName());
        response.setDisplayOrder(entity.getDisplayOrder());
        response.setIsActive(entity.getIsActive());
        return response;
    }

    private String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private String resolveCode(String rawCode, String name) {
        String code = trimmed(rawCode);
        return normalizeCode(code.isEmpty() ? name : code);
    }

    /** Bỏ dấu tiếng Việt để mã hạng mục chỉ còn A-Z, 0-9 và dấu gạch dưới. */
    private String normalizeCode(String raw) {
        String noAccent = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        String code = noAccent.toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        return code.isEmpty() ? "SAFETY_ITEM" : code;
    }

    private String uniqueCode(String baseCode) {
        String code = baseCode;
        int suffix = 2;
        while (workCategoryRepository.existsByCategoryCode(code)) {
            code = baseCode + "_" + suffix++;
        }
        return code;
    }
}
