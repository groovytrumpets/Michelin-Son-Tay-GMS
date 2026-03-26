package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.SpecAttributeJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.SpecificationJpa;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecAttributeJpaRepo extends JpaRepository<SpecAttributeJpa,Integer> {
}
