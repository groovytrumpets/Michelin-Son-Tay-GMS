package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import com.g42.platform.gms.warehouse.domain.entity.StockAllocation;
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
    public Optional<StockAllocation> findById(Integer allocationId) {
        return jpaRepo.findById(allocationId).map(this::toDomain);
    }

    @Override
    public List<StockAllocation> findByTicketAndStatus(Integer serviceTicketId, AllocationStatus status) {
        return jpaRepo.findByServiceTicketIdAndStatus(serviceTicketId, status)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<StockAllocation> findByTicketAndWarehouseAndStatus(Integer serviceTicketId, Integer warehouseId, AllocationStatus status) {
        return jpaRepo.findByServiceTicketIdAndWarehouseIdAndStatus(serviceTicketId, warehouseId, status)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<StockAllocation> findByIssueIdAndStatus(Integer issueId, AllocationStatus status) {
        return jpaRepo.findByIssueIdAndStatus(issueId, status)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<StockAllocation> findByEstimateItemId(Integer estimateItemId) {
        return jpaRepo.findByEstimateItemId(estimateItemId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public StockAllocation save(StockAllocation allocation) {
        StockAllocationJpa saved = jpaRepo.save(toJpa(allocation));
        return toDomain(saved);
    }

    private StockAllocation toDomain(StockAllocationJpa jpa) {
        StockAllocation domain = new StockAllocation();
        domain.setAllocationId(jpa.getAllocationId());
        domain.setServiceTicketId(jpa.getServiceTicketId());
        domain.setIssueId(jpa.getIssueId());
        domain.setEstimateItemId(jpa.getEstimateItemId());
        domain.setWarehouseId(jpa.getWarehouseId());
        domain.setItemId(jpa.getItemId());
        domain.setQuantity(jpa.getQuantity());
        domain.setStatus(jpa.getStatus());
        domain.setCreatedBy(jpa.getCreatedBy());
        domain.setCreatedAt(jpa.getCreatedAt());
        domain.setUpdatedAt(jpa.getUpdatedAt());
        return domain;
    }

    private StockAllocationJpa toJpa(StockAllocation domain) {
        StockAllocationJpa jpa = new StockAllocationJpa();
        jpa.setAllocationId(domain.getAllocationId());
        jpa.setServiceTicketId(domain.getServiceTicketId());
        jpa.setIssueId(domain.getIssueId());
        jpa.setEstimateItemId(domain.getEstimateItemId());
        jpa.setWarehouseId(domain.getWarehouseId());
        jpa.setItemId(domain.getItemId());
        jpa.setQuantity(domain.getQuantity());
        jpa.setStatus(domain.getStatus());
        jpa.setCreatedBy(domain.getCreatedBy());
        jpa.setCreatedAt(domain.getCreatedAt());
        jpa.setUpdatedAt(domain.getUpdatedAt());
        return jpa;
    }
}
