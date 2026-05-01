package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.InventoryTransaction;
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
    public InventoryTransaction save(InventoryTransaction transaction) {
        InventoryTransactionJpa saved = jpaRepo.save(toJpa(transaction));
        return toDomain(saved);
    }

    private InventoryTransactionJpa toJpa(InventoryTransaction domain) {
        InventoryTransactionJpa jpa = new InventoryTransactionJpa();
        jpa.setTransactionId(domain.getTransactionId());
        jpa.setWarehouseId(domain.getWarehouseId());
        jpa.setItemId(domain.getItemId());
        jpa.setEntryItemId(domain.getEntryItemId());
        jpa.setTransactionType(domain.getTransactionType());
        jpa.setQuantity(domain.getQuantity());
        jpa.setBalanceAfter(domain.getBalanceAfter());
        jpa.setReferenceType(domain.getReferenceType());
        jpa.setReferenceId(domain.getReferenceId());
        jpa.setNotes(domain.getNotes());
        jpa.setCreatedById(domain.getCreatedById());
        jpa.setCreatedAt(domain.getCreatedAt());
        return jpa;
    }

    private InventoryTransaction toDomain(InventoryTransactionJpa jpa) {
        InventoryTransaction domain = new InventoryTransaction();
        domain.setTransactionId(jpa.getTransactionId());
        domain.setWarehouseId(jpa.getWarehouseId());
        domain.setItemId(jpa.getItemId());
        domain.setEntryItemId(jpa.getEntryItemId());
        domain.setTransactionType(jpa.getTransactionType());
        domain.setQuantity(jpa.getQuantity());
        domain.setBalanceAfter(jpa.getBalanceAfter());
        domain.setReferenceType(jpa.getReferenceType());
        domain.setReferenceId(jpa.getReferenceId());
        domain.setNotes(jpa.getNotes());
        domain.setCreatedById(jpa.getCreatedById());
        domain.setCreatedAt(jpa.getCreatedAt());
        return domain;
    }
}
