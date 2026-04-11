package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryTransactionJpa;

public interface InventoryTransactionRepo {

    InventoryTransactionJpa save(InventoryTransactionJpa transaction);
}
