package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.ProductUnitJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductUnitJpaRepo extends JpaRepository<ProductUnitJpa, Integer> {
    ProductUnitJpa findByUnitName(String unitName);
    boolean existsByUnitName(String unitName);
    List<ProductUnitJpa> findAllByIsActive(Byte isActive);
}
