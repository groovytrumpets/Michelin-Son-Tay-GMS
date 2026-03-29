package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CatalogItemJpaRepo extends JpaRepository<CatalogItemJpa,Integer>, JpaSpecificationExecutor<CatalogItemJpa> {
    boolean existsBySku(String sku);
}
