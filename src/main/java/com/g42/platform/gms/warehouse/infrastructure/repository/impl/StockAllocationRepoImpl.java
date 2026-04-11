package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import com.g42.platform.gms.warehouse.domain.repository.StockAllocationRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockAllocationJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockAllocationJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("warehouseStockAllocationRepo")
@RequiredArgsConstructor
public class StockAllocationRepoImpl implements StockAllocationRepo {

    private final StockAllocationJpaRepo jpaRepo;

    @Override
    public Optional<StockAllocationJpa> findById(Integer allocationId) {
        return jpaRepo.findById(allocationId);
    }

    @Override
    public List<StockAllocationJpa> findByTicketAndStatus(Integer serviceTicketId, AllocationStatus status) {
        return jpaRepo.findByServiceTicketIdAndStatus(serviceTicketId, status);
    }

    @Override
    public StockAllocationJpa save(StockAllocationJpa allocation) {
        return jpaRepo.save(allocation);
    }
}
