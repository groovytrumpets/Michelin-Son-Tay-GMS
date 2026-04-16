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

import java.util.*;
import java.util.stream.Collectors;

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
        List<EstimateItemJpa> allItems = estimateItemJpaRepository.findByEstimateId(estimateId);
        List<StockAllocationJpa> allAllocations = stockAllocationRepositoryJpa.findAllByEstimateId(estimateId);

        // Chỉ lấy active items để trả về frontend
        List<EstimateItemJpa> activeItems = allItems.stream()
                .filter(i -> Boolean.FALSE.equals(i.getIsRemoved()))
                .toList();

        // Map allocation theo estimateItemId để lookup O(1)
        Map<Integer, StockAllocationJpa> allocationByItemId = allAllocations.stream()
                .collect(Collectors.toMap(
                        StockAllocationJpa::getEstimateItemId,
                        a -> a,
                        (a1, a2) -> a1
                ));
        Map<Integer, EstimateItemJpa> itemById = allItems.stream()
                .collect(Collectors.toMap(EstimateItemJpa::getId, i -> i));

        return activeItems.stream().map(activeItem -> {
            EstimateViaAllocationDto dto = new EstimateViaAllocationDto();
            dto.setEstimateItemDto(estimateDtoMapper.toEstimateItemDtoJpa(activeItem));

            StockAllocationJpa allocation = resolveAllocation(
                    activeItem,
                    allocationByItemId,
                    itemById
            );

            dto.setStockAllocationDto(allocation != null
                    ? stockAllocationJpaMapper.toDto(allocation)
                    : null);

            return dto;
        }).toList();
    }
    private StockAllocationJpa resolveAllocation(
            EstimateItemJpa item,
            Map<Integer, StockAllocationJpa> allocationByItemId,
            Map<Integer, EstimateItemJpa> itemById
    ) {
        Integer currentId = item.getId();
        Set<Integer> visited = new HashSet<>();

        while (currentId != null && !visited.contains(currentId)) {
            visited.add(currentId);

            // 1. check allocation tại node hiện tại
            StockAllocationJpa allocation = allocationByItemId.get(currentId);
            if (allocation != null) {
                return allocation;
            }

            // 2. đi về item trước
            EstimateItemJpa current = itemById.get(currentId);
            if (current == null) break;

            currentId = current.getRevisedFromItemId();
        }

        return null;
    }
}
