package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import com.g42.platform.gms.warehouse.domain.entity.StockAllocation;

import java.util.List;
import java.util.Optional;

public interface StockAllocationRepo {

    Optional<StockAllocation> findById(Integer allocationId);

    List<StockAllocation> findByTicketAndStatus(Integer serviceTicketId, AllocationStatus status);

    List<StockAllocation> findByTicketAndWarehouseAndStatus(Integer serviceTicketId, Integer warehouseId, AllocationStatus status);

    List<StockAllocation> findByIssueIdAndStatus(Integer issueId, AllocationStatus status);

    List<StockAllocation> findByEstimateItemId(Integer estimateItemId);

    StockAllocation save(StockAllocation allocation);
}
