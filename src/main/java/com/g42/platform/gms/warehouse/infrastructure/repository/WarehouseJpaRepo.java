package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseJpaRepo extends JpaRepository<WarehouseJpa,Integer> {
}
