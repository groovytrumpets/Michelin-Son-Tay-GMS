package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.domain.repository.PartCatalogRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Infrastructure Adapter: implements PartCatalogRepo (domain port).
 *
 * Bảng catalog_item chứa tất cả loại item: PART (phụ tùng), SERVICE (dịch vụ), COMBO...
 * PartCatalogRepo chỉ quan tâm đến PART — tất cả query đều filter itemType = PART.
 *
 * Method quan trọng nhất với StockEntryService là findNamesByIds():
 *
 * Luồng trong toResponse() của StockEntryService:
 *   1. Collect tất cả itemId từ entry.getItems() → Set<Integer> itemIds
 *   2. partCatalogRepo.findNamesByIds(itemIds.stream().toList())
 *        → PartCatalogRepoImpl.findNamesByIds(ids)
 *        → jpaRepo.findByItemTypeAndItemIdIn(PART, ids)
 *        → SQL: SELECT * FROM catalog_item
 *                WHERE item_type = 'PART' AND item_id IN (?, ?, ?, ...)
 *        → stream().collect(toMap(itemId → itemName))
 *        → trả về Map<Integer, String> {itemId → itemName}
 *   3. Với mỗi item trong response: itemName = map.get(item.getItemId())
 *
 * Tại sao dùng batch lookup (findNamesByIds) thay vì query từng cái?
 *   Phiếu nhập có 10 items → nếu query từng cái = 10 queries (N+1 problem)
 *   Dùng IN clause = 1 query cho tất cả → hiệu năng tốt hơn nhiều
 *
 * searchParts() dùng in-memory filter (không dùng SQL LIKE):
 *   - Load tất cả PART → filter trong Java
 *   - Phù hợp khi catalog không quá lớn (vài nghìn items)
 *   - Nếu catalog lớn hơn, nên chuyển sang SQL LIKE hoặc full-text search
 */
@Repository
@RequiredArgsConstructor
public class PartCatalogRepoImpl implements PartCatalogRepo {

    private final CatalogItemJpaRepo jpaRepo;

    /**
     * Lấy tất cả PART trong catalog.
     * SQL: SELECT * FROM catalog_item WHERE item_type = 'PART'
     * Dùng cho: Excel export catalog, dropdown chọn sản phẩm khi tạo phiếu nhập.
     */
    @Override
    public List<CatalogItem> findAllParts() {
        return jpaRepo.findByItemType(CatalogItemType.PART).stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * Tìm kiếm PART theo keyword — in-memory filter.
     * SQL: SELECT * FROM catalog_item WHERE item_type = 'PART'
     * Sau đó filter trong Java theo: itemName, sku, partNumber, barcode
     *
     * Lưu ý: load toàn bộ PART rồi filter → không scale tốt nếu catalog lớn.
     */
    @Override
    public List<CatalogItem> searchParts(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        return jpaRepo.findByItemType(CatalogItemType.PART).stream()
                .filter(c -> normalized.isEmpty()
                        || containsIgnoreCase(c.getItemName(), normalized)
                        || containsIgnoreCase(c.getSku(), normalized)
                        || containsIgnoreCase(c.getPartNumber(), normalized)
                        || containsIgnoreCase(c.getBarcode(), normalized))
                .map(this::toDomain)
                .toList();
    }

    /**
     * Lấy danh sách PART theo list ID.
     * SQL: SELECT * FROM catalog_item WHERE item_type = 'PART' AND item_id IN (?, ?, ...)
     * Trả về: empty list nếu ids null/empty (tránh SQL IN với list rỗng)
     */
    @Override
    public List<CatalogItem> findAllPartsByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return jpaRepo.findByItemTypeAndItemIdIn(CatalogItemType.PART, ids).stream()
                .map(this::toDomain)
                .toList();
    }

    /** SQL: SELECT COUNT(*) > 0 FROM catalog_item WHERE sku = ? */
    @Override
    public boolean existsBySku(String sku) {
        return jpaRepo.existsBySku(sku);
    }

    /** SQL: INSERT hoặc UPDATE catalog_item */
    @Override
    public CatalogItem save(CatalogItem item) {
        CatalogItemJpa saved = jpaRepo.save(toJpa(item));
        return toDomain(saved);
    }

    /**
     * Batch lookup: lấy tên sản phẩm theo list ID — dùng cho enrichment response.
     *
     * SQL: SELECT * FROM catalog_item WHERE item_type = 'PART' AND item_id IN (?, ?, ...)
     * Sau đó collect thành Map<itemId, itemName> trong Java.
     *
     * Trả về: empty Map nếu ids null/empty
     * Dùng bởi: StockEntryService.toResponse() để hiển thị tên sản phẩm trong response
     */
    @Override
    public Map<Integer, String> findNamesByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return jpaRepo.findByItemTypeAndItemIdIn(CatalogItemType.PART, ids).stream()
                .collect(Collectors.toMap(CatalogItemJpa::getItemId, CatalogItemJpa::getItemName));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean containsIgnoreCase(String value, String keywordLower) {
        return value != null && value.toLowerCase().contains(keywordLower);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    /** CatalogItemJpa → CatalogItem domain (map toàn bộ fields) */
    private CatalogItem toDomain(CatalogItemJpa jpa) {
        CatalogItem domain = new CatalogItem();
        domain.setItemId(jpa.getItemId());
        domain.setItemName(jpa.getItemName());
        domain.setItemType(jpa.getItemType());
        domain.setIsActive(jpa.getIsActive());
        domain.setWarrantyDurationMonths(jpa.getWarrantyDurationMonths());
        domain.setServiceServiceId(jpa.getServiceId());
        domain.setSku(jpa.getSku());
        domain.setPrice(jpa.getPrice());
        domain.setShowPrice(jpa.getShowPrice());
        domain.setDescription(jpa.getDescription());
        domain.setImageUrl(jpa.getImageUrl());
        domain.setUnit(jpa.getUnit());
        domain.setComboDurationMonths(jpa.getComboDurationMonths());
        domain.setComboDescription(jpa.getComboDescription());
        domain.setIsRecurring(jpa.getIsRecurring());
        domain.setBrandId(jpa.getBrandId());
        domain.setProductLineId(jpa.getProductLineId());
        domain.setMadeIn(jpa.getMadeIn());
        domain.setTaxRuleId(jpa.getTaxRuleId());
        domain.setWorkCategoryId(jpa.getWorkCategoryId());
        domain.setPartNumber(jpa.getPartNumber());
        domain.setBarcode(jpa.getBarcode());
        domain.setColor(jpa.getColor());
        return domain;
    }

    /** CatalogItem domain → CatalogItemJpa */
    private CatalogItemJpa toJpa(CatalogItem domain) {
        CatalogItemJpa jpa = new CatalogItemJpa();
        jpa.setItemId(domain.getItemId());
        jpa.setItemName(domain.getItemName());
        jpa.setItemType(domain.getItemType());
        jpa.setIsActive(domain.getIsActive());
        jpa.setWarrantyDurationMonths(domain.getWarrantyDurationMonths());
        jpa.setServiceId(domain.getServiceServiceId());
        jpa.setSku(domain.getSku());
        jpa.setPrice(domain.getPrice());
        jpa.setShowPrice(domain.getShowPrice());
        jpa.setDescription(domain.getDescription());
        jpa.setImageUrl(domain.getImageUrl());
        jpa.setUnit(domain.getUnit());
        jpa.setComboDurationMonths(domain.getComboDurationMonths());
        jpa.setComboDescription(domain.getComboDescription());
        jpa.setIsRecurring(domain.getIsRecurring());
        jpa.setBrandId(domain.getBrandId());
        jpa.setProductLineId(domain.getProductLineId());
        jpa.setMadeIn(domain.getMadeIn());
        jpa.setTaxRuleId(domain.getTaxRuleId());
        jpa.setWorkCategoryId(domain.getWorkCategoryId());
        jpa.setPartNumber(domain.getPartNumber());
        jpa.setBarcode(domain.getBarcode());
        jpa.setColor(domain.getColor());
        return jpa;
    }
}