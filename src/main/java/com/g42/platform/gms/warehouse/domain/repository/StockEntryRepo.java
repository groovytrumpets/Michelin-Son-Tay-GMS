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
 * Repository Port - lưu trữ phiếu nhập kho (StockEntry) và lô nhập (StockEntryItem).
 *
 * Trong luồng Issue, repo này dùng để:
 * - Lấy lô FIFO khi tạo/confirm phiếu xuất
 * - Trừ remaining_quantity của lô khi xuất hàng
 * - Truy ngược thông tin lô để hiển thị detail
 */
public interface StockEntryRepo {

    Optional<StockEntry> findById(Integer entryId);

    List<StockEntry> findByWarehouseId(Integer warehouseId);

    List<StockEntry> findByWarehouseIdAndStatus(Integer warehouseId, StockEntryStatus status);

    Page<StockEntry> search(Integer warehouseId,
                            StockEntryStatus status,
                            LocalDate fromDate,
                            LocalDate toDate,
                            String search,
                            Pageable pageable);

    StockEntry save(StockEntry entry);

    boolean existsByCode(String entryCode);

    /**
     * FIFO: lấy các lô còn hàng của item trong kho, cũ nhất trước.
     * Dùng trong create/update draft để chia quantity theo từng lô.
     */
    List<StockEntryItem> findFifoLots(Integer warehouseId, Integer itemId);

    /** Lấy lô nhập gần nhất — dùng để tham khảo giá nhập lần trước */
    Optional<StockEntryItem> findLatestLot(Integer warehouseId, Integer itemId);

    StockEntryItem saveItem(StockEntryItem item);

    /** Tìm lô item theo id (entry_item_id). */
    Optional<StockEntryItem> findItemById(Integer entryItemId);

    /**
     * Trừ remaining_quantity trực tiếp bằng UPDATE query.
     * Dùng lúc confirm issue để chốt việc xuất hàng khỏi từng lô.
     */
    int decreaseRemainingQuantity(Integer entryItemId, int qty);

    /** Tăng remaining_quantity trực tiếp bằng UPDATE query (khi hoàn hàng vào lô) */
    int increaseRemainingQuantity(Integer entryItemId, int qty);

    /** Giá fallback mới nhất của item trong kho — dùng cho StockEntryServiceImpl */
    BigDecimal findLatesFallBackPrice(Integer itemId, Integer warehouseId);

    /** Lấy entry theo entryCode */
    Optional<StockEntry> findByEntryCode(String entryCode);

    /** Tất cả lô còn hàng trong kho (PART only) — dùng cho Excel sync export */
    List<StockEntryItem> findActiveLotsByWarehouse(Integer warehouseId);

    /** Invalidate tất cả lô SYNC cũ — đặt remainingQuantity = 0 */
    void invalidateSyncLotsByWarehouse(Integer warehouseId);

    /** Lấy phiếu nhập theo id để enrich thông tin lot trong màn detail issue. */
    Optional<StockEntry> findEntryById(Integer entryId);
}
