package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.repository.InventoryTransactionRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryTransactionJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.InventoryTransactionJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InventoryTransactionRepoImpl implements InventoryTransactionRepo {

    private final InventoryTransactionJpaRepo jpaRepo;

    @Override
    public InventoryTransactionJpa save(InventoryTransactionJpa transaction) {
        return jpaRepo.save(transaction);
    }
}
