package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockAllocationJpa;

import java.util.List;
import java.util.Optional;

public interface StockAllocationRepo {

    Optional<StockAllocationJpa> findById(Integer allocationId);

    List<StockAllocationJpa> findByTicketAndStatus(Integer serviceTicketId, AllocationStatus status);

    List<StockAllocationJpa> findByTicketAndWarehouseAndStatus(Integer serviceTicketId, Integer warehouseId, AllocationStatus status);

    List<StockAllocationJpa> findByIssueIdAndStatus(Integer issueId, AllocationStatus status);

    List<StockAllocationJpa> findByEstimateItemId(Integer estimateItemId);

    StockAllocationJpa save(StockAllocationJpa allocation);
}
