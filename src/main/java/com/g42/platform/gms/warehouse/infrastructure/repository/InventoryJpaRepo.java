package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockTransferJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryJpaRepo extends JpaRepository<InventoryJpa,Integer> {
}
