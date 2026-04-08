package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReturnEntryJpaRepo extends JpaRepository<ReturnEntryJpa, Integer> {

    Optional<ReturnEntryJpa> findByReturnCode(String returnCode);

    List<ReturnEntryJpa> findByWarehouseIdOrderByCreatedAtDesc(Integer warehouseId);

    boolean existsByReturnCode(String returnCode);
}
