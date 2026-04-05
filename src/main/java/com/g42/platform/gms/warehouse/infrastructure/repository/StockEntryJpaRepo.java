package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.StockEntryJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockEntryJpaRepo extends JpaRepository<StockEntryJpa, Integer> {

    Optional<StockEntryJpa> findByEntryCode(String entryCode);

    boolean existsByEntryCode(String entryCode);
}
