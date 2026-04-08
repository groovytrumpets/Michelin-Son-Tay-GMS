package com.g42.platform.gms.estimation.domain.repository;

import com.g42.platform.gms.estimation.domain.entity.StockAllocation;
import org.springframework.stereotype.Repository;

@Repository
public interface StockAllocationRepository {
    StockAllocation createNewAllocation(StockAllocation stockAllocation);

    void updateReleasedOldEstimate(Integer revisedFromId);
}
