package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.enums.StockEntryStatus;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockEntryItemJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockEntryJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StockEntryRepoImpl implements StockEntryRepo {

    private final StockEntryJpaRepo jpaRepo;
    private final StockEntryItemJpaRepo itemJpaRepo;

    @Override
    public Optional<StockEntryJpa> findById(Integer entryId) {
        return jpaRepo.findById(entryId);
    }

    @Override
    public List<StockEntryJpa> findByWarehouseId(Integer warehouseId) {
        return jpaRepo.findByWarehouseIdOrderByCreatedAtDesc(warehouseId);
    }

    @Override
    public List<StockEntryJpa> findByWarehouseIdAndStatus(Integer warehouseId, StockEntryStatus status) {
        return jpaRepo.findByWarehouseIdAndStatusOrderByCreatedAtDesc(warehouseId, status);
    }

    @Override
    public StockEntryJpa save(StockEntryJpa entry) {
        return jpaRepo.save(entry);
    }

    @Override
    public boolean existsByCode(String entryCode) {
        return jpaRepo.existsByEntryCode(entryCode);
    }

    @Override
    public List<StockEntryItemJpa> findFifoLots(Integer warehouseId, Integer itemId) {
        return itemJpaRepo.findFifoLots(warehouseId, itemId);
    }

    @Override
    public java.util.Optional<StockEntryItemJpa> findLatestLot(Integer warehouseId, Integer itemId) {
        return itemJpaRepo.findLatestLot(warehouseId, itemId).stream().findFirst();
    }

    @Override
    public StockEntryItemJpa saveItem(StockEntryItemJpa item) {
        return itemJpaRepo.save(item);
    }

    @Override
    public java.util.Optional<StockEntryItemJpa> findItemById(Integer entryItemId) {
        return itemJpaRepo.findById(entryItemId);
    }

    @Override
    public int decreaseRemainingQuantity(Integer entryItemId, int qty) {
        return itemJpaRepo.decreaseRemainingQuantity(entryItemId, qty);
    }
}
