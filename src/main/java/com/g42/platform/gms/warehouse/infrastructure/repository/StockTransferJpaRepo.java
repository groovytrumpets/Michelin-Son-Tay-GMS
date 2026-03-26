package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.StockTransferJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTransferJpaRepo extends JpaRepository<StockTransferJpa,Integer> {
}
