package com.g42.platform.gms.estimation.app.service;

import com.g42.platform.gms.estimation.api.dto.StockAllocationDto;
import com.g42.platform.gms.estimation.api.mapper.StockAllocationDtoMapper;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.entity.StockAllocation;
import com.g42.platform.gms.estimation.domain.repository.EstimateItemRepository;
import com.g42.platform.gms.estimation.domain.repository.EstimateRepository;
import com.g42.platform.gms.estimation.domain.repository.StockAllocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockAllocationService {
    private final EstimateService estimateService;
    private final EstimateRepository estimateRepository;
    private final EstimateItemRepository estimateItemRepository;
    private final StockAllocationRepository stockAllocationRepository;
    private final StockAllocationDtoMapper stockAllocationDtoMapper;
    @Transactional
    public List<StockAllocationDto> createStockAllocation(Integer estimateId) {
        //todo: cancel
        Estimate estimate = estimateService.findById(estimateId);
//        //todo: RELEASED pervious estimate if not null
//        if (estimate.getRevisedFromId() != null && estimate.getVersion()>1) {
//            stockAllocationRepository.updateReleasedOldEstimate(estimate.getRevisedFromId());
//        }
        List<EstimateItem> estimateItems = estimateItemRepository.findByEstimateId(estimateId);
        //todo: RELEASED pervious estimate if not null
            stockAllocationRepository.updateReleasedOldEstimate(estimate.getRevisedFromId());

        List<StockAllocation> stockAllocations = new ArrayList<>();
        for (EstimateItem estimateItem : estimateItems) {
            if (estimateItem.getItemId()!=null && estimateItem.getWarehouseId()!=null) {
        StockAllocation stockAllocation = new StockAllocation();
        stockAllocation.setServiceTicketId(estimate.getServiceTicketId());
        stockAllocation.setEstimateItemId(estimateItem.getId());
        stockAllocation.setWarehouseId(estimateItem.getWarehouseId());
        stockAllocation.setItemId(estimateItem.getItemId());
        stockAllocation.setQuantity(estimateItem.getQuantity());
        stockAllocation.setEstimateId(estimateId);
        stockAllocation.setStatus("RESERVED");
//        stockAllocation.setCreatedBy();
        stockAllocation.setCreatedAt(Instant.now());
        StockAllocation savedStockAllocation = stockAllocationRepository.createNewAllocation(stockAllocation);
                stockAllocations.add(savedStockAllocation);
            }
        }
        return stockAllocations.stream().map(stockAllocationDtoMapper::toDto).toList();
    }
}
