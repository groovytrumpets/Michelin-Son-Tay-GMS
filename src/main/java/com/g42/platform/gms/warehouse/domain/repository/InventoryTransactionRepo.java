package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.InventoryTransaction;

public interface InventoryTransactionRepo {

    InventoryTransaction save(InventoryTransaction transaction);
}
