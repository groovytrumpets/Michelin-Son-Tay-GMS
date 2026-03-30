package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import com.g42.platform.gms.warehouse.domain.entity.WarehousePricing;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehousePricingJpaRepo extends JpaRepository<WarehousePricingJpa,Integer> {
}
