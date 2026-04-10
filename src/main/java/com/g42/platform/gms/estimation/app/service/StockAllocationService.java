package com.g42.platform.gms.estimation.app.service;

import com.g42.platform.gms.estimation.api.dto.StockAllocationDto;
import com.g42.platform.gms.estimation.api.mapper.StockAllocationDtoMapper;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.entity.StockAllocation;
import com.g42.platform.gms.estimation.domain.repository.EstimateItemRepository;
import com.g42.platform.gms.estimation.domain.repository.EstimateRepository;
import com.g42.platform.gms.estimation.domain.repository.StockAllocationRepository;
import com.g42.platform.gms.warehouse.api.internal.WarehouseInternalApi;
import com.g42.platform.gms.warehouse.app.service.inventory.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockAllocationService {
    private final EstimateService estimateService;
    private final EstimateRepository estimateRepository;
    private final EstimateItemRepository estimateItemRepository;
    private final StockAllocationRepository stockAllocationRepository;
    private final StockAllocationDtoMapper stockAllocationDtoMapper;
    private final WarehouseInternalApi warehouseInternalApi;
    private final InventoryService inventoryService;

    @Transactional
    public List<StockAllocationDto> createStockAllocation(Integer estimateId, Integer staffId) {
        //todo: cancel
        Estimate estimate = estimateService.findById(estimateId);
//        //todo: RELEASED pervious estimate if not null
//        if (estimate.getRevisedFromId() != null && estimate.getVersion()>1) {
//            stockAllocationRepository.updateReleasedOldEstimate(estimate.getRevisedFromId());
//        }
        if (estimate.getRevisedFromId() != null) {
            List<StockAllocation> oldAllocations = stockAllocationRepository.findByEstimateId(estimate.getRevisedFromId());

            for (StockAllocation oldAlloc : oldAllocations) {
                if ("RESERVED".equals(oldAlloc.getStatus())) {
                    inventoryService.decreaseReservedQuantity(
                            oldAlloc.getItemId(),
                            oldAlloc.getWarehouseId(),
                            oldAlloc.getQuantity()
                    );
                }
            }


        //todo: RELEASED pervious estimate if not null
            stockAllocationRepository.updateReleasedOldEstimate(estimate.getRevisedFromId());
        }
        List<EstimateItem> estimateItems = estimateItemRepository.findByEstimateId(estimateId);
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
        stockAllocation.setCreatedBy(staffId);
//        stockAllocation.setCreatedBy();
        stockAllocation.setCreatedAt(Instant.now());
        StockAllocation savedStockAllocation = stockAllocationRepository.createNewAllocation(stockAllocation);
                stockAllocations.add(savedStockAllocation);
            //todo: update Inventory
                inventoryService.increaseReservedQuantity(
                    savedStockAllocation.getItemId(),
                    savedStockAllocation.getWarehouseId(),
                    savedStockAllocation.getQuantity());
            }
        }
        return stockAllocations.stream().map(stockAllocationDtoMapper::toDto).toList();
    }
    @Transactional
    public List<StockAllocationDto> updateStockAllocation(Integer estimateId, Integer staffId, List<StockAllocationDto> stockAllocationDtos) {
        List<StockAllocation> oldList = stockAllocationRepository.findByEstimateId(estimateId);
        Map<Integer, StockAllocation> oldMap = oldList.stream()
                .collect(Collectors.toMap(
                        StockAllocation::getAllocationId,
                        allocation -> allocation
                ));
        //handle add new and update:
        for (StockAllocationDto dto : stockAllocationDtos) {
        if (dto.getAllocationId()==null){
            //add new
            StockAllocation stockAllocationNew = stockAllocationDtoMapper.toDomain(dto);
            stockAllocationNew.setCreatedBy(staffId);
            stockAllocationNew.setCreatedAt(Instant.now());
            stockAllocationNew.setStatus("RESERVED");
            stockAllocationRepository.save(stockAllocationNew);
            //increase inventory
            inventoryService.increaseReservedQuantity(dto.getItemId(),dto.getWarehouseId(),dto.getQuantity());

        }else if (oldMap.containsKey(dto.getAllocationId())) {
        //update
            StockAllocation oldAllocation = oldMap.get(dto.getAllocationId());
            //count difference Delta: old 4 tire new 6 tire update: +2 tire
            //if old 4 tire, new 1 tire mean -3 tire
            int difference = dto.getQuantity() - oldAllocation.getQuantity();
            if (difference != 0) {
                oldAllocation.setQuantity(dto.getQuantity());
                stockAllocationRepository.save(oldAllocation);

                inventoryService.updateReservedQuantityByDelta(dto.getItemId(),dto.getWarehouseId(),difference);
            }
            oldMap.remove(dto.getAllocationId());
        }
        for (StockAllocation deletedAlloc : oldMap.values()) {
            inventoryService.decreaseReservedQuantity(deletedAlloc.getItemId(),deletedAlloc.getWarehouseId(),deletedAlloc.getQuantity());

            stockAllocationRepository.delete(deletedAlloc);
        }
        }
        return stockAllocationRepository.findByEstimateId(estimateId).stream().map(stockAllocationDtoMapper::toDto).toList();
    }
}
