package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.domain.entity.StockAllocation;
import com.g42.platform.gms.estimation.domain.repository.StockAllocationRepository;
import com.g42.platform.gms.estimation.infrastructure.entity.StockAllocationJpa;
import com.g42.platform.gms.estimation.infrastructure.mapper.StockAllocationJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.StockAllocationRepositoryJpa;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockAllocationRepoImpl implements StockAllocationRepository {
    private final StockAllocationJpaMapper stockAllocationJpaMapper;
    private final StockAllocationRepositoryJpa stockAllocationRepositoryJpa;

    public StockAllocationRepoImpl(StockAllocationJpaMapper stockAllocationJpaMapper, StockAllocationRepositoryJpa stockAllocationRepositoryJpa) {
        this.stockAllocationJpaMapper = stockAllocationJpaMapper;
        this.stockAllocationRepositoryJpa = stockAllocationRepositoryJpa;
    }

    @Override
    public StockAllocation createNewAllocation(StockAllocation stockAllocation) {
        System.out.println("DEBUG: createNewAllocation");
        stockAllocation.toString();
        StockAllocationJpa stockAllocationJpa = stockAllocationRepositoryJpa.save(stockAllocationJpaMapper.fromDomain(stockAllocation));
        return stockAllocationJpaMapper.toDomain(stockAllocationJpa);
    }

    @Override
    public void updateReleasedOldEstimate(Integer revisedFromId) {
        stockAllocationRepositoryJpa.updateReleasedEstimateById(revisedFromId);
    }

    @Override
    public List<StockAllocation> findByEstimateId(Integer estimateId) {
        List<StockAllocationJpa> stockAllocationJpas = stockAllocationRepositoryJpa.findAllByEstimateId(estimateId);
        return stockAllocationJpas.stream().map(stockAllocationJpaMapper::toDomain).toList();
    }

    @Override
    public StockAllocation findByEstimateItemId(Integer estimateItemId) {
        StockAllocationJpa stockAllocationJpa = stockAllocationRepositoryJpa.findByEstimateItemId(estimateItemId);
        if (stockAllocationJpa == null) {
            return null;
        }
        return stockAllocationJpaMapper.toDomain(stockAllocationJpa);
    }

    @Override
    public StockAllocation findByEstimateIdAndWarehouseIdAndItemIdAndStatus(
            Integer estimateId, Integer warehouseId, Integer itemId, String status) {
        StockAllocationJpa stockAllocationJpa = stockAllocationRepositoryJpa
                .findByEstimateIdAndWarehouseIdAndItemIdAndStatus(estimateId, warehouseId, itemId, status);
        if (stockAllocationJpa == null) {
            return null;
        }
        return stockAllocationJpaMapper.toDomain(stockAllocationJpa);
    }

    @Override
    public void save(StockAllocation stockAllocationNew) {
        if (stockAllocationNew.getAllocationId() != null) {

        StockAllocationJpa stockAllocationJpa = stockAllocationRepositoryJpa.getStockAllocationJpaByAllocationId(stockAllocationNew.getAllocationId());
        if (stockAllocationJpa == null) {
            new RuntimeException("Không tìm thấy Allocation ID:"+stockAllocationNew.getAllocationId());
        }
        stockAllocationJpa.setQuantity(stockAllocationNew.getQuantity());
        stockAllocationRepositoryJpa.save(stockAllocationJpa);
        }else {
            StockAllocationJpa stockAllocationJpa = stockAllocationJpaMapper.fromDomain(stockAllocationNew);
            stockAllocationRepositoryJpa.save(stockAllocationJpa);
        }
    }

    @Override
    public void delete(StockAllocation deletedAlloc) {
        StockAllocationJpa stockAllocationJpa = stockAllocationJpaMapper.fromDomain(deletedAlloc);
        stockAllocationRepositoryJpa.delete(stockAllocationJpa);
    }
}
