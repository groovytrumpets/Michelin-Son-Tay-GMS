package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockEntryItemJpaRepo extends JpaRepository<StockEntryItemJpa, Integer> {

       /** Lấy toàn bộ item theo phiếu nhập. */
    List<StockEntryItemJpa> findByEntryId(Integer entryId);

       /**
        * FIFO lot selection.
        * Chỉ lấy lô còn remainingQuantity > 0, phiếu nhập phải CONFIRMED,
        * rồi sắp xếp theo entryItemId tăng dần để xuất lô cũ trước.
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

    @Modifying
    @Query("UPDATE StockEntryItemJpa sei SET sei.remainingQuantity = sei.remainingQuantity - :qty WHERE sei.entryItemId = :id AND sei.remainingQuantity >= :qty")
    int decreaseRemainingQuantity(@Param("id") Integer entryItemId, @Param("qty") int qty);

        /** Tăng remainingQuantity khi hoàn hàng vào lô. */
        @Modifying
        @Query("UPDATE StockEntryItemJpa sei SET sei.remainingQuantity = sei.remainingQuantity + :qty WHERE sei.entryItemId = :id")
        int increaseRemainingQuantity(@Param("id") Integer entryItemId, @Param("qty") int qty);

        /** Tất cả lô còn hàng trong kho — dùng để kiểm tra tổng quan FIFO. */
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
