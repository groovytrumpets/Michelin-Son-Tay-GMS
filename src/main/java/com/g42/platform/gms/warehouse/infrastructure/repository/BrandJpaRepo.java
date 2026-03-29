package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.BrandJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandJpaRepo extends JpaRepository<BrandJpa,Integer> {
    BrandJpa findByBrandName(String brandName);

    BrandJpa findByBrandNameContainingIgnoreCase(String brandName);
}
