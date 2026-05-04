package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA Repository cho StockEntryItem (chi tiết lô hàng nhập).
 *
 * FIFO Logic (findFifoLots):
 * - Trả về danh sách lô còn hàng theo thứ tự FIFO (First-In-First-Out).
 * - ORDER BY sei.entryItemId ASC = lô cũ nhất trước (vì entryItemId tăng dần theo thời gian).
 * - Service StockIssueService.create() dùng FIFO này để:
 *   1. Tính giá nhập (importPrice) từ lô cũ nhất
 *   2. Xác định lô nào sẽ xuất trước
 *   3. Khi confirm(), giảm remainingQuantity từ lô cũ nhất
 *
 * @Modifying Updates:
 * - decreaseRemainingQuantity / increaseRemainingQuantity dùng UPDATE SQL trực tiếp
 *   (không load entity, không Hibernate overwrite), đảm bảo:
 *   - Khi confirm xuất kho: remainingQuantity chỉ bị trừ, không bị overwrite
 *   - Khi hoàn trả: remainingQuantity được cộng lại
 *
 * Ghi chú về Concurrency:
 * - remainingQuantity là thuộc tính CRITICAL (dùng cho FIFO allocation)
 * - Phải dùng @Modifying UPDATE, không được load+save (tránh race condition)
 */
public interface StockEntryItemJpaRepo extends JpaRepository<StockEntryItemJpa, Integer> {

    List<StockEntryItemJpa> findByEntryId(Integer entryId);

    /**
     * FIFO Lots: lấy các lô còn hàng, cũ nhất trước.
     * Điều kiện:
     * - remainingQuantity > 0 (lô còn hàng)
     * - status = CONFIRMED (entry đã nhập kho)
     * - warehouse + itemId trùng
     * ORDER BY entryItemId ASC = cũ nhất trước (FIFO)
     *
     * Service sẽ duyệt lô này từ đầu, consume từng lô cho đến hết demand.
     */
    @Query("SELECT sei FROM StockEntryItemJpa sei " +
           "JOIN StockEntryJpa se ON se.entryId = sei.entryId " +
           "WHERE se.warehouseId = :warehouseId " +
           "  AND sei.itemId = :itemId " +
           "  AND sei.remainingQuantity > 0 " +
           "  AND se.status = com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus.CONFIRMED " +
           "ORDER BY sei.entryItemId ASC")
    List<StockEntryItemJpa> findFifoLots(
            @Param("warehouseId") Integer warehouseId,
            @Param("itemId") Integer itemId);

    /** Lấy lô nhập gần nhất (mới nhất) — dùng để tham khảo giá nhập lần trước */
    @Query("SELECT sei FROM StockEntryItemJpa sei " +
           "JOIN StockEntryJpa se ON se.entryId = sei.entryId " +
           "WHERE se.warehouseId = :warehouseId " +
           "  AND sei.itemId = :itemId " +
           "  AND se.status = com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus.CONFIRMED " +
           "ORDER BY sei.entryItemId DESC")
    List<StockEntryItemJpa> findLatestLot(
            @Param("warehouseId") Integer warehouseId,
            @Param("itemId") Integer itemId);

    /**
     * Giảm remainingQuantity bằng UPDATE SQL (không load entity).
     *
     * Luồng confirm xuất kho:
     * 1. StockIssueService.confirm() lấy issue items
     * 2. Với mỗi item có entryItemId: gọi repo.decreaseRemainingQuantity(entryItemId, qty)
     * 3. Query: UPDATE lô SET remainingQuantity -= qty
     * 4. Return 1 (success) hoặc 0 (không đủ hàng)
     *
     * Tại sao @Modifying UPDATE? Tránh race condition:
     * - Cách load+save: Request A load=100, Request B load=100 →
     *   A save(50) overwrite, B save(70) overwrite → kết quả sai
     * - Cách @Modifying: UPDATE atomic → database handle → kết quả đúng
     */
    @Modifying
    @Query("UPDATE StockEntryItemJpa sei SET sei.remainingQuantity = sei.remainingQuantity - :qty WHERE sei.entryItemId = :id AND sei.remainingQuantity >= :qty")
    int decreaseRemainingQuantity(@Param("id") Integer entryItemId, @Param("qty") int qty);

    /**
     * Tăng remainingQuantity bằng UPDATE SQL (không load entity).
     *
     * Luồng hoàn trả hàng:
     * 1. ReturnEntryService.confirm() lấy return items
     * 2. Cộng remainingQuantity lại cho lô (khi hoàn trả customer return)
     * 3. Lô lại khả dụng cho FIFO allocation tiếp theo
     */
    @Modifying
    @Query("UPDATE StockEntryItemJpa sei SET sei.remainingQuantity = sei.remainingQuantity + :qty WHERE sei.entryItemId = :id")
       int increaseRemainingQuantity(@Param("id") Integer entryItemId, @Param("qty") int qty);

    /** Tất cả lô còn hàng trong kho — dùng để kiểm tra tổng quan FIFO */
    @Query("SELECT sei FROM StockEntryItemJpa sei " +
           "JOIN StockEntryJpa se ON se.entryId = sei.entryId " +
           "JOIN WarehouseCatalogItem ci ON ci.itemId = sei.itemId " +
           "WHERE se.warehouseId = :warehouseId " +
           "  AND sei.remainingQuantity > 0 " +
           "  AND se.status = com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus.CONFIRMED " +
           "  AND ci.itemType = com.g42.platform.gms.warehouse.domain.enums.CatalogItemType.PART " +
           "ORDER BY sei.itemId ASC, sei.entryItemId ASC")
    List<StockEntryItemJpa> findActiveLotsByWarehouse(@Param("warehouseId") Integer warehouseId);

    /**
     * Invalidate tất cả lô SYNC cũ của warehouse — đặt remainingQuantity = 0.
     * Gọi trước khi import sync mới để FIFO không dùng lô cũ nữa.
     */
    @Modifying
    @Query("UPDATE StockEntryItemJpa sei SET sei.remainingQuantity = 0 " +
           "WHERE sei.entryId IN (" +
           "  SELECT se.entryId FROM StockEntryJpa se " +
           "  WHERE se.warehouseId = :warehouseId " +
           "    AND se.supplierName = 'SYNC - Đồng bộ từ kho T3'" +
           ")")
    void invalidateSyncLotsByWarehouse(@Param("warehouseId") Integer warehouseId);

    /** Giá nhập mới nhất theo từng item trong kho — dùng cho export sync */
    @Query("SELECT sei.itemId, sei.importPrice FROM StockEntryItemJpa sei " +
           "JOIN StockEntryJpa se ON se.entryId = sei.entryId " +
           "WHERE se.warehouseId = :warehouseId " +
           "  AND se.status = com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus.CONFIRMED " +
           "  AND sei.entryItemId = (" +
           "    SELECT MAX(sei2.entryItemId) FROM StockEntryItemJpa sei2 " +
           "    JOIN StockEntryJpa se2 ON se2.entryId = sei2.entryId " +
           "    WHERE se2.warehouseId = :warehouseId AND sei2.itemId = sei.itemId " +
           "      AND se2.status = com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus.CONFIRMED" +
           "  )")
    List<Object[]> findLatestImportPriceByWarehouse(@Param("warehouseId") Integer warehouseId);

    /** Markup mới nhất theo từng item trong kho — dùng cho export sync */
    @Query("SELECT sei.itemId, sei.markupMultiplier FROM StockEntryItemJpa sei " +
           "JOIN StockEntryJpa se ON se.entryId = sei.entryId " +
           "WHERE se.warehouseId = :warehouseId " +
           "  AND se.status = com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus.CONFIRMED " +
           "  AND sei.entryItemId = (" +
           "    SELECT MAX(sei2.entryItemId) FROM StockEntryItemJpa sei2 " +
           "    JOIN StockEntryJpa se2 ON se2.entryId = sei2.entryId " +
           "    WHERE se2.warehouseId = :warehouseId AND sei2.itemId = sei.itemId " +
           "      AND se2.status = com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus.CONFIRMED" +
           "  )")
    List<Object[]> findLatestMarkupByWarehouse(@Param("warehouseId") Integer warehouseId);
}
