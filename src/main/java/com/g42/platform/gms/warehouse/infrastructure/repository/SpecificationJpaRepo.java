package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.ProductLineJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.SpecificationJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpecificationJpaRepo extends JpaRepository<SpecificationJpa,Integer> {
    List<SpecificationJpa> findAllByItemId(Integer itemId);
}
