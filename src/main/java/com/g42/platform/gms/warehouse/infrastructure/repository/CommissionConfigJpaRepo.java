package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.CommissionConfigJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommissionConfigJpaRepo extends JpaRepository<CommissionConfigJpa, Integer> {

    Optional<CommissionConfigJpa> findByItemIdAndIsActiveTrue(Integer itemId);
}
