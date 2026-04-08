package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockEntryItemJpaRepo extends JpaRepository<StockEntryItemJpa, Integer> {

    List<StockEntryItemJpa> findByEntryId(Integer entryId);

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
}
