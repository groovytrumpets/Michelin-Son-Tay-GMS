package com.g42.platform.gms.estimation.domain.repository;

import com.g42.platform.gms.estimation.domain.entity.StockAllocation;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAllocationRepository {
    StockAllocation createNewAllocation(StockAllocation stockAllocation);

    void updateReleasedOldEstimate(Integer revisedFromId);

    List<StockAllocation> findByEstimateId(Integer estimateId);

    StockAllocation findByEstimateItemId(Integer estimateItemId);

    StockAllocation findByEstimateIdAndWarehouseIdAndItemIdAndStatus(
            Integer estimateId, Integer warehouseId, Integer itemId, String status);

    void save(StockAllocation stockAllocationNew);

    void delete(StockAllocation deletedAlloc);
}
