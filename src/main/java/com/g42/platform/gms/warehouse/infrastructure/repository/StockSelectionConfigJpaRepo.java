package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.StockSelectionConfigJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockSelectionConfigJpaRepo extends JpaRepository<StockSelectionConfigJpa, Integer> {

    Optional<StockSelectionConfigJpa> findFirstByIsActiveTrueOrderByConfigIdDesc();
}
