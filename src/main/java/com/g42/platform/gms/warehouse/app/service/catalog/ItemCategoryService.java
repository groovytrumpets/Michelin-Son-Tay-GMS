package com.g42.platform.gms.warehouse.app.service.catalog;

import com.g42.platform.gms.warehouse.api.dto.ItemCategoryAssignmentDto;
import com.g42.platform.gms.warehouse.api.dto.ItemCategoryDto;
import com.g42.platform.gms.warehouse.api.dto.request.AssignItemCategoryRequest;
import com.g42.platform.gms.warehouse.api.mapper.ItemCategoryDtoMapper;
import com.g42.platform.gms.warehouse.domain.entity.ItemCategory;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ItemCategoryJpa;
import com.g42.platform.gms.warehouse.infrastructure.mapper.ItemCategoryEntityJpaMapper;
import com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.ItemCategoryJpaRepo;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Quản lý danh mục phụ tùng / dịch vụ (bảng item_category).
 *
 * Danh mục là TÙY CHỌN: một phụ tùng hay dịch vụ có thể không thuộc danh mục nào,
 * và bảng báo giá vẫn chọn được sản phẩm mà chưa cần chọn danh mục trước. Danh mục
 * chỉ để nhóm hàng hóa cho dễ tra và để khai thuế mặc định cho nhóm.
 *
 * Danh mục đã dùng ở phiếu cũ thì chỉ ẩn (is_active = false) chứ không xóa cứng.
 */
@Service
@RequiredArgsConstructor
public class ItemCategoryService {

    private final ItemCategoryJpaRepo itemCategoryJpaRepo;
    private final CatalogItemJpaRepo catalogItemJpaRepo;
    private final ItemCategoryEntityJpaMapper entityMapper;
    private final ItemCategoryDtoMapper dtoMapper;

    /** Danh sách cho màn cấu hình, gồm cả danh mục đã ẩn. */
    public List<ItemCategoryDto> getAllForConfig() {
        return itemCategoryJpaRepo.findAll().stream()
                .map(entityMapper::toDomain)
                .sorted(byDisplayOrderThenName())
                .map(dtoMapper::toDto)
                .toList();
    }

    /** Danh sách đang dùng, để đổ vào ô chọn danh mục ở các màn nhập liệu. */
    public List<ItemCategoryDto> getActive() {
        return itemCategoryJpaRepo.findAllByIsActiveTrueOrderByDisplayOrderAscCategoryNameAsc().stream()
                .map(entityMapper::toDomain)
                .map(dtoMapper::toDto)
                .toList();
    }

    public List<ItemCategory> getActiveDomain() {
        return itemCategoryJpaRepo.findAllByIsActiveTrueOrderByDisplayOrderAscCategoryNameAsc().stream()
                .map(entityMapper::toDomain)
                .toList();
    }

    @Transactional
    public ItemCategoryDto create(ItemCategory request) {
        String name = trimmed(request.getCategoryName());
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }
        if (itemCategoryJpaRepo.findFirstByCategoryNameIgnoreCase(name) != null) {
            throw new IllegalArgumentException("Danh mục \"" + name + "\" đã tồn tại.");
        }

        ItemCategoryJpa entity = new ItemCategoryJpa();
        entity.setCategoryName(name);
        entity.setCategoryCode(uniqueCode(resolveCode(request.getCategoryCode(), name)));
        entity.setCategoryType(normalizeType(request.getCategoryType()));
        entity.setTaxRuleId(request.getTaxRuleId());
        entity.setIsActive(request.getIsActive() == null || request.getIsActive());
        entity.setDisplayOrder(request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : itemCategoryJpaRepo.findMaxDisplayOrder() + 1);

        return dtoMapper.toDto(entityMapper.toDomain(itemCategoryJpaRepo.save(entity)));
    }

    @Transactional
    public ItemCategoryDto update(Integer categoryId, ItemCategory request) {
        ItemCategoryJpa entity = itemCategoryJpaRepo.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục #" + categoryId));

        String name = trimmed(request.getCategoryName());
        if (!name.isEmpty()) {
            ItemCategoryJpa duplicated = itemCategoryJpaRepo.findFirstByCategoryNameIgnoreCase(name);
            if (duplicated != null && !duplicated.getItemCategoryId().equals(categoryId)) {
                throw new IllegalArgumentException("Danh mục \"" + name + "\" đã tồn tại.");
            }
            entity.setCategoryName(name);
        }
        String code = trimmed(request.getCategoryCode());
        if (!code.isEmpty()) entity.setCategoryCode(normalizeCode(code));
        if (request.getCategoryType() != null) entity.setCategoryType(normalizeType(request.getCategoryType()));
        if (request.getDisplayOrder() != null) entity.setDisplayOrder(request.getDisplayOrder());
        if (request.getTaxRuleId() != null) entity.setTaxRuleId(request.getTaxRuleId());
        if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());

        return dtoMapper.toDto(entityMapper.toDomain(itemCategoryJpaRepo.save(entity)));
    }

    /**
     * Danh mục còn hàng hóa đang trỏ tới thì chỉ ẩn; hoàn toàn chưa dùng thì xóa hẳn
     * để danh sách không phình ra vì những lần tạo nhầm.
     */
    @Transactional
    public void deactivate(Integer categoryId) {
        ItemCategoryJpa entity = itemCategoryJpaRepo.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục #" + categoryId));

        if (catalogItemJpaRepo.existsByItemCategoryId(categoryId)) {
            entity.setIsActive(false);
            itemCategoryJpaRepo.save(entity);
        } else {
            itemCategoryJpaRepo.deleteById(categoryId);
        }
    }

    /**
     * Danh sách hàng hóa kèm danh mục hiện tại, phục vụ màn xếp danh mục hàng loạt.
     *
     * @param itemType     lọc PART / SERVICE / COMBO; để trống là lấy tất cả
     * @param categoryId   chỉ lấy hàng thuộc đúng danh mục này
     * @param uncategorized true = chỉ lấy hàng CHƯA có danh mục (ưu tiên hơn categoryId,
     *                      vì "chưa xếp" và "thuộc danh mục X" loại trừ nhau)
     */
    public Page<ItemCategoryAssignmentDto> searchItemsForAssignment(String search,
                                                                    String itemType,
                                                                    Integer categoryId,
                                                                    Boolean uncategorized,
                                                                    Boolean isActive,
                                                                    int page,
                                                                    int size) {
        Specification<CatalogItemJpa> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            CatalogItemType type = parseItemType(itemType);
            if (type != null) predicates.add(cb.equal(root.get("itemType"), type));
            if (isActive != null) predicates.add(cb.equal(root.get("isActive"), isActive));

            if (Boolean.TRUE.equals(uncategorized)) {
                predicates.add(cb.isNull(root.get("itemCategoryId")));
            } else if (categoryId != null) {
                predicates.add(cb.equal(root.get("itemCategoryId"), categoryId));
            }

            String keyword = trimmed(search).toLowerCase();
            if (!keyword.isEmpty()) {
                String like = "%" + keyword + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("itemName")), like),
                        cb.like(cb.lower(root.get("sku")), like),
                        cb.like(cb.lower(root.get("partNumber")), like),
                        cb.like(cb.lower(root.get("barcode")), like)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        int safeSize = size <= 0 ? 20 : Math.min(size, 200);
        int safePage = Math.max(page, 0);
        Page<CatalogItemJpa> found = catalogItemJpaRepo.findAll(
                spec, PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "itemName")));

        // Tra tên danh mục một lượt cho cả trang thay vì mỗi dòng một truy vấn
        Map<Integer, String> categoryNameById = itemCategoryJpaRepo
                .findAllById(found.getContent().stream()
                        .map(CatalogItemJpa::getItemCategoryId)
                        .filter(java.util.Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(ItemCategoryJpa::getItemCategoryId, ItemCategoryJpa::getCategoryName,
                        (a, b) -> a));

        return found.map(item -> new ItemCategoryAssignmentDto(
                item.getItemId(),
                item.getItemName(),
                item.getSku(),
                item.getPartNumber(),
                item.getItemType() == null ? null : item.getItemType().name(),
                item.getIsActive(),
                item.getItemCategoryId(),
                item.getItemCategoryId() == null ? null : categoryNameById.get(item.getItemCategoryId())
        ));
    }

    /**
     * Xếp nhiều món vào cùng một danh mục, hoặc gỡ danh mục nếu itemCategoryId để trống.
     *
     * @return số món thực sự được cập nhật
     */
    @Transactional
    public int assignCategory(AssignItemCategoryRequest request) {
        List<Integer> itemIds = request.getItemIds() == null ? List.of() : request.getItemIds().stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        if (itemIds.isEmpty()) return 0;

        Integer categoryId = request.getItemCategoryId();
        if (categoryId != null && !itemCategoryJpaRepo.existsById(categoryId)) {
            throw new IllegalArgumentException("Không tìm thấy danh mục #" + categoryId);
        }

        List<CatalogItemJpa> items = catalogItemJpaRepo.findAllById(itemIds);
        items.forEach(item -> item.setItemCategoryId(categoryId));
        catalogItemJpaRepo.saveAll(items);
        return items.size();
    }

    /** Chuỗi loại hàng không hợp lệ thì coi như không lọc, thay vì ném lỗi vào mặt người dùng. */
    private CatalogItemType parseItemType(String rawType) {
        String type = trimmed(rawType).toUpperCase();
        if (type.isEmpty()) return null;
        try {
            return CatalogItemType.valueOf(type);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Tìm theo tên, tạo mới nếu chưa có. Dùng cho luồng nhập kho từ Excel — ở đó
     * người dùng gõ tên danh mục trong file chứ không chọn từ danh sách.
     */
    @Transactional
    public ItemCategory findOrCreateByName(String categoryName) {
        String name = trimmed(categoryName);
        if (name.isEmpty()) return null;

        ItemCategoryJpa existing = itemCategoryJpaRepo.findFirstByCategoryNameIgnoreCase(name);
        if (existing != null) return entityMapper.toDomain(existing);

        ItemCategory request = new ItemCategory();
        request.setCategoryName(name);
        ItemCategoryDto created = create(request);
        return entityMapper.toDomain(itemCategoryJpaRepo.findById(created.getItemCategoryId()).orElseThrow());
    }

    private Comparator<ItemCategory> byDisplayOrderThenName() {
        return Comparator
                .comparingInt((ItemCategory c) -> c.getDisplayOrder() == null ? Integer.MAX_VALUE : c.getDisplayOrder())
                .thenComparing(c -> String.valueOf(c.getCategoryName()), String.CASE_INSENSITIVE_ORDER);
    }

    private String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    /** Chỉ nhận PART hoặc SERVICE; giá trị khác coi như không phân loại. */
    private String normalizeType(String rawType) {
        String type = trimmed(rawType).toUpperCase();
        return "PART".equals(type) || "SERVICE".equals(type) ? type : null;
    }

    private String resolveCode(String rawCode, String name) {
        String code = trimmed(rawCode);
        return normalizeCode(code.isEmpty() ? name : code);
    }

    /** Bỏ dấu tiếng Việt để mã danh mục chỉ còn A-Z, 0-9 và dấu gạch dưới. */
    private String normalizeCode(String raw) {
        String noAccent = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd').replace('Đ', 'D');
        String code = noAccent.toUpperCase().replaceAll("[^A-Z0-9]+", "_").replaceAll("^_+|_+$", "");
        return code.isEmpty() ? "CATEGORY" : code;
    }

    /** Mã bị trùng thì thêm hậu tố số, tránh ném lỗi vào mặt người dùng vì một chi tiết kỹ thuật. */
    private String uniqueCode(String baseCode) {
        String code = baseCode;
        int suffix = 2;
        while (itemCategoryJpaRepo.existsByCategoryCode(code)) {
            code = baseCode + "_" + suffix++;
        }
        return code;
    }
}
