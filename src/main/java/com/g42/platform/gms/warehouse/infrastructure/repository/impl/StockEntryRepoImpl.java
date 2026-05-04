package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.StockEntry;
import com.g42.platform.gms.warehouse.domain.entity.StockEntryItem;
import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockEntryItemJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockEntryJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Infrastructure Adapter: implements StockEntryRepo (domain port).
 *
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  Kiến trúc tầng (Layered Architecture)                          │
 * │                                                                  │
 * │  StockEntryService                                               │
 * │       │ gọi interface                                            │
 * │       ▼                                                          │
 * │  StockEntryRepo (domain interface)                               │
 * │       │ Spring inject impl                                        │
 * │       ▼                                                          │
 * │  StockEntryRepoImpl  ◄── bạn đang ở đây                         │
 * │       │ gọi JPA repos                                            │
 * │       ├──► StockEntryJpaRepo    → SQL trên bảng stock_entry      │
 * │       └──► StockEntryItemJpaRepo → SQL trên bảng stock_entry_item│
 * │                                                                  │
 * │  Mỗi method = 3 bước:                                           │
 * │    1. Gọi JPA repo (Spring Data / JPQL / @Modifying)            │
 * │    2. Map JPA entity → domain entity (toDomain)                  │
 * │    3. Trả về domain entity cho Service                           │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * Tại sao cần tầng này?
 * - Service không phụ thuộc vào JPA/Hibernate → dễ test (mock interface)
 * - Có thể đổi DB (PostgreSQL → MongoDB) mà không sửa Service
 * - Tách biệt domain logic và persistence logic
 *
 * Ghi chú quan trọng về CascadeType:
 * - StockEntryJpa có @OneToMany(cascade = CascadeType.ALL) với items
 * - Khi save(StockEntryJpa), Hibernate tự INSERT/UPDATE/DELETE items
 * - Impl phải sync items vào JPA entity trước khi gọi jpaRepo.save()
 *
 * Ghi chú về @Modifying UPDATE (decreaseRemainingQuantity):
 * - Dùng UPDATE SQL trực tiếp thay vì load+save để tránh race condition
 * - Xem chi tiết tại StockEntryItemJpaRepo
 */
@Repository
@RequiredArgsConstructor
public class StockEntryRepoImpl implements StockEntryRepo {

    private final StockEntryJpaRepo jpaRepo;         // Spring Data JPA cho bảng stock_entry
    private final StockEntryItemJpaRepo itemJpaRepo; // Spring Data JPA cho bảng stock_entry_item

    // ── Mappers: chuyển đổi giữa JPA entity và domain entity ─────────────────

    /**
     * JPA entity → Domain entity (StockEntryJpa → StockEntry).
     * Gọi đệ quy toDomainItem() cho từng item trong collection.
     */
    private StockEntry toDomain(StockEntryJpa jpa) {
        return StockEntry.builder()
                .entryId(jpa.getEntryId())
                .entryCode(jpa.getEntryCode())
                .warehouseId(jpa.getWarehouseId())
                .supplierName(jpa.getSupplierName())
                .entryDate(jpa.getEntryDate())
                .status(jpa.getStatus())
                .notes(jpa.getNotes())
                .confirmedBy(jpa.getConfirmedBy())
                .confirmedAt(jpa.getConfirmedAt())
                .createdBy(jpa.getCreatedBy())
                .createdAt(jpa.getCreatedAt())
                .updatedAt(jpa.getUpdatedAt())
                .items(jpa.getItems() != null
                        ? jpa.getItems().stream().map(this::toDomainItem).toList()
                        : new ArrayList<>())
                .build();
    }

    /**
     * Domain entity → JPA entity (StockEntry → StockEntryJpa).
     * Chỉ map header, items được sync riêng trong save().
     */
    private StockEntryJpa toJpa(StockEntry domain) {
        StockEntryJpa jpa = new StockEntryJpa();
        jpa.setEntryId(domain.getEntryId());
        jpa.setEntryCode(domain.getEntryCode());
        jpa.setWarehouseId(domain.getWarehouseId());
        jpa.setSupplierName(domain.getSupplierName());
        jpa.setEntryDate(domain.getEntryDate());
        jpa.setStatus(domain.getStatus() != null ? domain.getStatus() : StockEntryStatus.DRAFT);
        jpa.setNotes(domain.getNotes());
        jpa.setConfirmedBy(domain.getConfirmedBy());
        jpa.setConfirmedAt(domain.getConfirmedAt());
        jpa.setCreatedBy(domain.getCreatedBy());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());
        return jpa;
    }

    /** StockEntryItemJpa → StockEntryItem domain */
    private StockEntryItem toDomainItem(StockEntryItemJpa jpa) {
        return StockEntryItem.builder()
                .entryItemId(jpa.getEntryItemId())
                .entryId(jpa.getEntryId())
                .itemId(jpa.getItemId())
                .quantity(jpa.getQuantity())
                .importPrice(jpa.getImportPrice())
                .markupMultiplier(jpa.getMarkupMultiplier())
                .remainingQuantity(jpa.getRemainingQuantity())
                .notes(jpa.getNotes())
                .build();
    }

    /** StockEntryItem domain → StockEntryItemJpa */
    private StockEntryItemJpa toJpaItem(StockEntryItem domain) {
        StockEntryItemJpa jpa = new StockEntryItemJpa();
        jpa.setEntryItemId(domain.getEntryItemId());
        jpa.setEntryId(domain.getEntryId());
        jpa.setItemId(domain.getItemId());
        jpa.setQuantity(domain.getQuantity());
        jpa.setImportPrice(domain.getImportPrice());
        jpa.setMarkupMultiplier(domain.getMarkupMultiplier() != null
                ? domain.getMarkupMultiplier() : BigDecimal.ONE);
        jpa.setRemainingQuantity(domain.getRemainingQuantity() != null
                ? domain.getRemainingQuantity() : 0);
        jpa.setNotes(domain.getNotes());
        return jpa;
    }

    // ── StockEntryRepo implementations ───────────────────────────────────────

    /**
     * Tìm phiếu nhập theo ID.
     * SQL: SELECT * FROM stock_entry WHERE entry_id = ?
     *      + Hibernate lazy/eager load items theo @OneToMany config
     * Trả về: Optional.empty() nếu không tìm thấy → Service throw 404
     */
    @Override
    public Optional<StockEntry> findById(Integer entryId) {
        return jpaRepo.findById(entryId).map(this::toDomain);
    }

    /**
     * Lấy tất cả phiếu nhập của kho, mới nhất trước.
     * SQL: SELECT * FROM stock_entry WHERE warehouse_id = ? ORDER BY created_at DESC
     * Dùng cho: export Excel (không phân trang)
     */
    @Override
    public List<StockEntry> findByWarehouseId(Integer warehouseId) {
        return jpaRepo.findByWarehouseIdOrderByCreatedAtDesc(warehouseId)
                .stream().map(this::toDomain).toList();
    }

    /**
     * Lấy phiếu nhập theo kho + trạng thái.
     * SQL: SELECT * FROM stock_entry WHERE warehouse_id = ? AND status = ? ORDER BY created_at DESC
     */
    @Override
    public List<StockEntry> findByWarehouseIdAndStatus(Integer warehouseId, StockEntryStatus status) {
        return jpaRepo.findByWarehouseIdAndStatusOrderByCreatedAtDesc(warehouseId, status)
                .stream().map(this::toDomain).toList();
    }

    /**
     * Tìm kiếm phiếu nhập có filter + phân trang.
     *
     * Trước khi gọi JPA: normalize search string (trim, null nếu blank)
     * để JPQL xử lý đúng điều kiện "search IS NULL → bỏ qua filter".
     *
     * JPQL trong StockEntryJpaRepo.search():
     *   WHERE warehouse_id = ?
     *     AND (:status IS NULL OR status = :status)
     *     AND (:fromDate IS NULL OR entry_date >= :fromDate)
     *     AND (:toDate IS NULL OR entry_date <= :toDate)
     *     AND (:search IS NULL OR LOWER(entry_code) LIKE %:search%
     *                          OR LOWER(supplier_name) LIKE %:search%)
     *   ORDER BY created_at DESC (từ Pageable)
     *
     * Trả về: Page<StockEntry> — Service dùng .map(toResponse) để convert
     */
    @Override
    public Page<StockEntry> search(Integer warehouseId,
                                   StockEntryStatus status,
                                   LocalDate fromDate,
                                   LocalDate toDate,
                                   String search,
                                   Pageable pageable) {
        // Normalize: "" → null để JPQL bỏ qua filter search khi không có keyword
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        return jpaRepo.search(warehouseId, status, fromDate, toDate, normalizedSearch, pageable)
                .map(this::toDomain);
    }

    /**
     * Lưu phiếu nhập kèm toàn bộ items.
     *
     * Luồng xử lý:
     * 1. toJpa(entry) → tạo StockEntryJpa (chưa có items)
     * 2. Sync items: clear collection cũ, add items mới đã map sang JPA
     *    (cần thiết vì CascadeType.ALL dựa vào collection trong JPA entity)
     * 3. jpaRepo.save(jpa):
     *    - Nếu entryId = null → INSERT INTO stock_entry + INSERT INTO stock_entry_item (cascade)
     *    - Nếu entryId != null → UPDATE stock_entry + INSERT/UPDATE/DELETE items (cascade)
     * 4. toDomain(saved) → trả về domain entity với entryId đã được DB sinh ra
     *
     * Lưu ý: Service gọi save() 2 lần khi tạo mới:
     *   Lần 1: items = empty → INSERT header → lấy entryId
     *   Lần 2: items = đầy đủ → UPDATE header + INSERT items
     */
    @Override
    public StockEntry save(StockEntry entry) {
        StockEntryJpa jpa = toJpa(entry);

        // Sync items vào JPA entity để CascadeType.ALL hoạt động đúng
        if (entry.getItems() != null) {
            jpa.getItems().clear(); // xóa collection cũ (Hibernate sẽ DELETE items cũ nếu orphanRemoval=true)
            for (StockEntryItem item : entry.getItems()) {
                StockEntryItemJpa itemJpa = toJpaItem(item);
                if (itemJpa.getEntryId() == null) {
                    itemJpa.setEntryId(entry.getEntryId()); // gán entryId nếu item mới
                }
                jpa.getItems().add(itemJpa);
            }
        }

        return toDomain(jpaRepo.save(jpa));
    }

    /**
     * Kiểm tra mã phiếu đã tồn tại chưa.
     * SQL: SELECT COUNT(*) > 0 FROM stock_entry WHERE entry_code = ?
     * Dùng trong generateCode() để tránh sinh mã trùng.
     */
    @Override
    public boolean existsByCode(String entryCode) {
        return jpaRepo.existsByEntryCode(entryCode);
    }

    /**
     * FIFO: lấy các lô còn hàng, cũ nhất trước.
     *
     * JPQL trong StockEntryItemJpaRepo.findFifoLots():
     *   SELECT sei FROM StockEntryItemJpa sei
     *   JOIN StockEntryJpa se ON se.entryId = sei.entryId
     *   WHERE se.warehouseId = ? AND sei.itemId = ?
     *     AND sei.remainingQuantity > 0
     *     AND se.status = CONFIRMED
     *   ORDER BY sei.entryItemId ASC   ← cũ nhất trước
     *
     * Dùng bởi: StockIssueService.confirm() để xuất hàng theo FIFO.
     */
    @Override
    public List<StockEntryItem> findFifoLots(Integer warehouseId, Integer itemId) {
        return itemJpaRepo.findFifoLots(warehouseId, itemId)
                .stream().map(this::toDomainItem).toList();
    }

    /**
     * Lấy lô nhập gần nhất (mới nhất).
     * JPQL: ORDER BY entryItemId DESC → lấy phần tử đầu tiên
     */
    @Override
    public Optional<StockEntryItem> findLatestLot(Integer warehouseId, Integer itemId) {
        return itemJpaRepo.findLatestLot(warehouseId, itemId)
                .stream().findFirst().map(this::toDomainItem);
    }

    /**
     * Lưu 1 item riêng lẻ (dùng cho patchItem).
     * SQL: UPDATE stock_entry_item SET ... WHERE entry_item_id = ?
     */
    @Override
    public StockEntryItem saveItem(StockEntryItem item) {
        return toDomainItem(itemJpaRepo.save(toJpaItem(item)));
    }

    /**
     * Tìm item theo ID.
     * SQL: SELECT * FROM stock_entry_item WHERE entry_item_id = ?
     */
    @Override
    public Optional<StockEntryItem> findItemById(Integer entryItemId) {
        return itemJpaRepo.findById(entryItemId).map(this::toDomainItem);
    }

    /**
     * Giảm remainingQuantity bằng UPDATE SQL trực tiếp.
     *
     * SQL: UPDATE stock_entry_item
     *      SET remaining_quantity = remaining_quantity - :qty
     *      WHERE entry_item_id = :id AND remaining_quantity >= :qty
     *
     * Trả về: 1 nếu thành công, 0 nếu không đủ hàng.
     * Service phải check return value để biết có đủ hàng không.
     */
    @Override
    public int decreaseRemainingQuantity(Integer entryItemId, int qty) {
        return itemJpaRepo.decreaseRemainingQuantity(entryItemId, qty);
    }

    /**
     * Tăng remainingQuantity bằng UPDATE SQL trực tiếp.
     * SQL: UPDATE stock_entry_item SET remaining_quantity = remaining_quantity + :qty WHERE entry_item_id = :id
     */
    @Override
    public int increaseRemainingQuantity(Integer entryItemId, int qty) {
        return itemJpaRepo.increaseRemainingQuantity(entryItemId, qty);
    }

    /**
     * Lấy giá bán fallback mới nhất.
     *
     * JPQL trong StockEntryJpaRepo.findLatesFallBackPrice():
     *   SELECT sei.importPrice * sei.markupMultiplier
     *   FROM StockEntryItemJpa sei JOIN StockEntryJpa se ON ...
     *   WHERE se.warehouseId = ? AND sei.itemId = ?
     *     AND sei.remainingQuantity > 0 AND se.status = CONFIRMED
     *   ORDER BY se.createdAt ASC LIMIT 1
     *
     * Trả về: null nếu chưa có lô nào → PricingService dùng giá catalog thay thế.
     */
    @Override
    public BigDecimal findLatesFallBackPrice(Integer itemId, Integer warehouseId) {
        return jpaRepo.findLatesFallBackPrice(itemId, warehouseId).orElse(null);
    }

    /** SQL: SELECT * FROM stock_entry WHERE entry_code = ? */
    @Override
    public Optional<StockEntry> findByEntryCode(String entryCode) {
        return jpaRepo.findByEntryCode(entryCode).map(this::toDomain);
    }

    /**
     * Lấy tất cả lô còn hàng trong kho (PART only).
     * JPQL: JOIN với CatalogItem để lọc itemType = PART, remainingQuantity > 0, status = CONFIRMED
     */
    @Override
    public List<StockEntryItem> findActiveLotsByWarehouse(Integer warehouseId) {
        return itemJpaRepo.findActiveLotsByWarehouse(warehouseId)
                .stream().map(this::toDomainItem).toList();
    }

    /**
     * Invalidate lô SYNC cũ — đặt remainingQuantity = 0.
     * SQL: UPDATE stock_entry_item SET remaining_quantity = 0
     *      WHERE entry_id IN (SELECT entry_id FROM stock_entry WHERE warehouse_id = ? AND supplier_name = 'SYNC...')
     */
    @Override
    public void invalidateSyncLotsByWarehouse(Integer warehouseId) {
        itemJpaRepo.invalidateSyncLotsByWarehouse(warehouseId);
    }

    /** Alias của findById */
    @Override
    public Optional<StockEntry> findEntryById(Integer entryId) {
        return jpaRepo.findById(entryId).map(this::toDomain);
    }
}