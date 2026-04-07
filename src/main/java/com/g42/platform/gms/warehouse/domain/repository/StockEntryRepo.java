package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryJpa;

import java.util.List;
import java.util.Optional;

public interface StockEntryRepo {

    Optional<StockEntryJpa> findById(Integer entryId);

    List<StockEntryJpa> findByWarehouseId(Integer warehouseId);

    List<StockEntryJpa> findByWarehouseIdAndStatus(Integer warehouseId, StockEntryStatus status);

    StockEntryJpa save(StockEntryJpa entry);

    boolean existsByCode(String entryCode);

    /** FIFO: lấy các lô còn hàng của item trong kho, cũ nhất trước */
    List<StockEntryItemJpa> findFifoLots(Integer warehouseId, Integer itemId);

    StockEntryItemJpa saveItem(StockEntryItemJpa item);

    /** Trừ remaining_quantity trực tiếp bằng UPDATE query — tránh Hibernate overwrite */
    int decreaseRemainingQuantity(Integer entryItemId, int qty);
}
