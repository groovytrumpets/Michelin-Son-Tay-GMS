package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ProductLineJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductLineJpaRepo extends JpaRepository<ProductLineJpa,Integer> {
}
