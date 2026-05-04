package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.StockEntry;
import com.g42.platform.gms.warehouse.domain.entity.StockEntryItem;
import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Domain port (interface) cho StockEntry.
 *
 * Đây là tầng trung gian giữa Service và Infrastructure:
 *   Service → StockEntryRepo (interface) → StockEntryRepoImpl (JPA adapter)
 *
 * Service chỉ biết interface này, không biết JPA hay SQL bên dưới.
 * Toàn bộ mapping JPA entity ↔ domain entity nằm trong StockEntryRepoImpl.
 *
 * Các method được nhóm theo mục đích:
 *   - Query phiếu nhập (findById, findByWarehouseId, search...)
 *   - Ghi phiếu nhập (save, saveItem)
 *   - FIFO lot management (findFifoLots, decreaseRemainingQuantity...)
 *   - Utility (existsByCode, findLatesFallBackPrice...)
 */
public interface StockEntryRepo {

    // ── Query phiếu nhập ─────────────────────────────────────────────────────

    /**
     * Tìm phiếu nhập theo ID, kèm toàn bộ items.
     * Impl: SELECT * FROM stock_entry WHERE entry_id = ? + load items
     */
    Optional<StockEntry> findById(Integer entryId);

    /**
     * Lấy tất cả phiếu nhập của 1 kho, mới nhất trước.
     * Impl: SELECT * FROM stock_entry WHERE warehouse_id = ? ORDER BY created_at DESC
     * Dùng cho: export Excel (cần lấy hết, không phân trang)
     */
    List<StockEntry> findByWarehouseId(Integer warehouseId);

    /**
     * Lấy phiếu nhập theo kho + trạng thái, mới nhất trước.
     * Impl: SELECT * FROM stock_entry WHERE warehouse_id = ? AND status = ? ORDER BY created_at DESC
     */
    List<StockEntry> findByWarehouseIdAndStatus(Integer warehouseId, StockEntryStatus status);

    /**
     * Tìm kiếm phiếu nhập có filter + phân trang.
     * Impl: JPQL với các điều kiện optional (null = bỏ qua filter đó):
     *   WHERE warehouse_id = ?
     *     AND (status IS NULL OR status = ?)
     *     AND (fromDate IS NULL OR entry_date >= ?)
     *     AND (toDate IS NULL OR entry_date <= ?)
     *     AND (search IS NULL OR entry_code LIKE ? OR supplier_name LIKE ?)
     *   ORDER BY created_at DESC (từ Pageable)
     */
    Page<StockEntry> search(Integer warehouseId,
                            StockEntryStatus status,
                            LocalDate fromDate,
                            LocalDate toDate,
                            String search,
                            Pageable pageable);

    // ── Ghi phiếu nhập ───────────────────────────────────────────────────────

    /**
     * Lưu phiếu nhập kèm toàn bộ items (INSERT hoặc UPDATE).
     *
     * Impl dùng CascadeType.ALL: khi save StockEntryJpa, Hibernate tự động
     * INSERT/UPDATE/DELETE các StockEntryItemJpa trong collection items.
     *
     * Lưu ý: save() được gọi 2 lần khi tạo mới:
     *   Lần 1: save header → lấy entryId từ DB (auto-generated)
     *   Lần 2: save lại kèm items đã có entryId
     */
    StockEntry save(StockEntry entry);

    /**
     * Lưu 1 item riêng lẻ (dùng cho patchItem — sửa từng dòng).
     * Impl: jpaRepo.save(itemJpa) → UPDATE stock_entry_item SET ... WHERE entry_item_id = ?
     */
    StockEntryItem saveItem(StockEntryItem item);

    // ── FIFO lot management ───────────────────────────────────────────────────

    /**
     * FIFO: lấy các lô còn hàng của item trong kho, cũ nhất trước.
     *
     * Impl SQL (JPQL):
     *   SELECT sei FROM StockEntryItemJpa sei
     *   JOIN StockEntryJpa se ON se.entryId = sei.entryId
     *   WHERE se.warehouseId = ? AND sei.itemId = ?
     *     AND sei.remainingQuantity > 0
     *     AND se.status = CONFIRMED
     *   ORDER BY sei.entryItemId ASC   ← cũ nhất trước (entryItemId tăng dần theo thời gian)
     *
     * Dùng bởi: StockIssueService.confirm() để tính giá xuất và trừ lô theo FIFO.
     * Ví dụ: cần xuất 80 cái, lô A còn 50, lô B còn 100
     *   → trừ lô A hết 50, trừ lô B thêm 30
     */
    List<StockEntryItem> findFifoLots(Integer warehouseId, Integer itemId);

    /**
     * Lấy lô nhập gần nhất (mới nhất) của item trong kho.
     * Impl: ORDER BY entryItemId DESC, lấy phần tử đầu tiên
     * Dùng để tham khảo giá nhập lần trước khi tạo phiếu nhập mới.
     */
    Optional<StockEntryItem> findLatestLot(Integer warehouseId, Integer itemId);

    /**
     * Tìm item theo ID — dùng cho patchItem để verify item thuộc đúng phiếu.
     * Impl: SELECT * FROM stock_entry_item WHERE entry_item_id = ?
     */
    Optional<StockEntryItem> findItemById(Integer entryItemId);

    /**
     * Giảm remainingQuantity bằng UPDATE SQL trực tiếp — KHÔNG load entity.
     *
     * Impl SQL:
     *   UPDATE stock_entry_item
     *   SET remaining_quantity = remaining_quantity - :qty
     *   WHERE entry_item_id = :id AND remaining_quantity >= :qty
     *
     * Tại sao dùng UPDATE thay vì load+save?
     *   Tránh race condition: nếu 2 request confirm cùng lúc cùng trừ 1 lô,
     *   cách load+save sẽ overwrite nhau → số lượng sai.
     *   Cách UPDATE atomic: DB xử lý tuần tự → kết quả đúng.
     *
     * Return: 1 nếu thành công, 0 nếu không đủ hàng (remainingQuantity < qty)
     * Dùng bởi: StockIssueService.confirm()
     */
    int decreaseRemainingQuantity(Integer entryItemId, int qty);

    /**
     * Tăng remainingQuantity bằng UPDATE SQL trực tiếp — KHÔNG load entity.
     *
     * Impl SQL:
     *   UPDATE stock_entry_item
     *   SET remaining_quantity = remaining_quantity + :qty
     *   WHERE entry_item_id = :id
     *
     * Dùng bởi: ReturnEntryService.confirm() khi khách trả hàng về lô cũ.
     */
    int increaseRemainingQuantity(Integer entryItemId, int qty);

    // ── Utility ──────────────────────────────────────────────────────────────

    /**
     * Kiểm tra mã phiếu đã tồn tại chưa — dùng trong generateCode() để tránh trùng.
     * Impl: SELECT COUNT(*) > 0 FROM stock_entry WHERE entry_code = ?
     */
    boolean existsByCode(String entryCode);

    /**
     * Lấy giá bán fallback mới nhất của item trong kho.
     *
     * Impl SQL (JPQL):
     *   SELECT sei.importPrice * sei.markupMultiplier
     *   FROM StockEntryItemJpa sei JOIN StockEntryJpa se ON ...
     *   WHERE se.warehouseId = ? AND sei.itemId = ?
     *     AND sei.remainingQuantity > 0
     *     AND se.status = CONFIRMED
     *   ORDER BY se.createdAt ASC LIMIT 1
     *
     * Dùng bởi: PricingService khi warehouse_pricing chưa được cấu hình.
     * Giá bán fallback = importPrice × markupMultiplier từ lô còn hàng cũ nhất.
     */
    BigDecimal findLatesFallBackPrice(Integer itemId, Integer warehouseId);

    /** Tìm phiếu nhập theo mã phiếu (entryCode). */
    Optional<StockEntry> findByEntryCode(String entryCode);

    /**
     * Lấy tất cả lô còn hàng trong kho (PART only).
     * Dùng cho: Excel sync export — xuất danh sách lô để đồng bộ với hệ thống ngoài.
     */
    List<StockEntryItem> findActiveLotsByWarehouse(Integer warehouseId);

    /**
     * Đặt remainingQuantity = 0 cho tất cả lô SYNC cũ của kho.
     * Gọi trước khi import sync mới để FIFO không dùng lô cũ nữa.
     * Impl: UPDATE stock_entry_item SET remaining_quantity = 0 WHERE entry_id IN (...)
     */
    void invalidateSyncLotsByWarehouse(Integer warehouseId);

    /** Alias của findById — dùng khi cần enrich lot info. */
    Optional<StockEntry> findEntryById(Integer entryId);
}
