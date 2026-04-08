package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.repository.ReturnEntryRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.ReturnEntryItemJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.ReturnEntryJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReturnEntryRepoImpl implements ReturnEntryRepo {

    private final ReturnEntryJpaRepo jpaRepo;
    private final ReturnEntryItemJpaRepo itemJpaRepo;

    @Override
    public Optional<ReturnEntryJpa> findById(Integer returnId) {
        return jpaRepo.findById(returnId);
    }

    @Override
    public List<ReturnEntryJpa> findByWarehouseId(Integer warehouseId) {
        return jpaRepo.findByWarehouseIdOrderByCreatedAtDesc(warehouseId);
    }

    @Override
    public ReturnEntryJpa save(ReturnEntryJpa entry) {
        return jpaRepo.save(entry);
    }

    @Override
    public boolean existsByCode(String returnCode) {
        return jpaRepo.existsByReturnCode(returnCode);
    }

    @Override
    public Optional<ReturnEntryItemJpa> findItemById(Integer returnItemId) {
        return itemJpaRepo.findById(returnItemId);
    }

    @Override
    public ReturnEntryItemJpa saveItem(ReturnEntryItemJpa item) {
        return itemJpaRepo.save(item);
    }
}
