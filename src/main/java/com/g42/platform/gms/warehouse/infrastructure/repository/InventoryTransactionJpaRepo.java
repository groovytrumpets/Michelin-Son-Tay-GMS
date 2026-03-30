package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryTransactionJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionJpaRepo extends JpaRepository<InventoryTransactionJpa,Integer> {
}
