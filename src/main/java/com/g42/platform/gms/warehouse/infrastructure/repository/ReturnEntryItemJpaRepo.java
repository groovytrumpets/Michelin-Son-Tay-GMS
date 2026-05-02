package com.g42.platform.gms.warehouse.infrastructure.repository;

import com.g42.platform.gms.warehouse.infrastructure.entity.ReturnEntryItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReturnEntryItemJpaRepo extends JpaRepository<ReturnEntryItemJpa, Integer> {

    List<ReturnEntryItemJpa> findByReturnId(Integer returnId);

    ReturnEntryItemJpa findByAllocationId(Integer allocationId);

    ReturnEntryItemJpa findTopByAllocationId(Integer allocationId);

    ReturnEntryItemJpa findTopByAllocationIdOrderByReturnItemIdDesc(Integer allocationId);
}
