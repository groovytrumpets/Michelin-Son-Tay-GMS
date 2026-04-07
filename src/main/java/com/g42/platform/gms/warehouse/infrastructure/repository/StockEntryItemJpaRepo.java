package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StockEntryItemJpaRepo extends JpaRepository<StockEntryItemJpa, Integer> {

    List<StockEntryItemJpa> findByEntryId(Integer entryId);

    /**
     * FIFO: lấy các lô còn hàng của item trong kho cụ thể, sắp xếp cũ nhất trước.
     * Join với stock_entry để lọc theo warehouseId và chỉ lấy phiếu CONFIRMED.
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
}
