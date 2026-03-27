package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogItemJpaRepo extends JpaRepository<CatalogItemJpa,Integer> {
    boolean existsBySku(String sku);
}
