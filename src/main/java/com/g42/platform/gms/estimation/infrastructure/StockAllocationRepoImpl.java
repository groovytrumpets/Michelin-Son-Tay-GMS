package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.api.dto.EstimateViaAllocationDto;
import com.g42.platform.gms.estimation.api.mapper.EstimateDtoMapper;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.entity.StockAllocation;
import com.g42.platform.gms.estimation.domain.repository.StockAllocationRepository;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateItemJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.StockAllocationJpa;
import com.g42.platform.gms.estimation.infrastructure.mapper.StockAllocationJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.StockAllocationRepositoryJpa;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StockAllocationRepoImpl implements StockAllocationRepository {
    private final StockAllocationJpaMapper stockAllocationJpaMapper;
    private final StockAllocationRepositoryJpa stockAllocationRepositoryJpa;
    private final EstimateItemJpaRepository estimateItemJpaRepository;
    private final EstimateDtoMapper estimateDtoMapper;

    public StockAllocationRepoImpl(StockAllocationJpaMapper stockAllocationJpaMapper, StockAllocationRepositoryJpa stockAllocationRepositoryJpa, EstimateItemJpaRepository estimateItemJpaRepository, EstimateDtoMapper estimateDtoMapper) {
        this.stockAllocationJpaMapper = stockAllocationJpaMapper;
        this.stockAllocationRepositoryJpa = stockAllocationRepositoryJpa;
        this.estimateItemJpaRepository = estimateItemJpaRepository;
        this.estimateDtoMapper = estimateDtoMapper;
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

    @Override
    public List<EstimateViaAllocationDto> findEstimateAndAllocationById(Integer estimateId) {
        List<EstimateViaAllocationDto> listEstimateViaAllocationDto = new ArrayList<>();
        List<EstimateItemJpa> estimateItems = estimateItemJpaRepository.findByEstimateIdAndIsRemoved(estimateId, false);
        List<StockAllocationJpa> stockAllocationJpas = stockAllocationRepositoryJpa.findAllByEstimateId(estimateId);
        for (EstimateItemJpa estimateItem : estimateItems) {
        EstimateViaAllocationDto  estimateViaAllocationDto = new EstimateViaAllocationDto();
            estimateViaAllocationDto.setEstimateItemDto(estimateDtoMapper.toEstimateItemDtoJpa(estimateItem));
            for (StockAllocationJpa stockAllocationJpa : stockAllocationJpas) {
                if (stockAllocationJpa.getEstimateItemId().equals(estimateItem.getId())) {
                    estimateViaAllocationDto.setStockAllocationDto(stockAllocationJpaMapper.toDto(stockAllocationJpa));
                }
            }
            listEstimateViaAllocationDto.add(estimateViaAllocationDto);
        }
        return listEstimateViaAllocationDto;
    }
}
