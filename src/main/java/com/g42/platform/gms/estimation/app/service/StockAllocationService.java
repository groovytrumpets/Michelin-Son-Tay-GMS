package com.g42.platform.gms.estimation.app.service;

import com.g42.platform.gms.estimation.api.dto.EstimateViaAllocationDto;
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
import java.util.*;
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
        Estimate newEstimate = estimateService.findById(estimateId);
        List<StockAllocation> stockAllocations = new ArrayList<>();
        Integer currentRevIdToCheck = newEstimate.getRevisedFromId();

        // =========================================================================
        // TRƯỜNG HỢP 1: TẠO VERSION MỚI TỪ VERSION CŨ (REVISION)
        // =========================================================================
        if (newEstimate.getRevisedFromId() != null) {
            List<StockAllocation> oldAllocations = new ArrayList<>();
            while (currentRevIdToCheck !=null){
                List<StockAllocation> oldAllocations2 = stockAllocationRepository.findByEstimateId(currentRevIdToCheck);
                oldAllocations.addAll(oldAllocations2);
                Estimate prevEstimate = estimateService.findById(currentRevIdToCheck);
                currentRevIdToCheck = prevEstimate.getRevisedFromId();
            }

            // 1. Lấy toàn bộ Allocation cũ của Ver 1 (Cả COMMITTED)

            List<EstimateItem> oldEstimateItems = estimateItemRepository.findByEstimateId(newEstimate.getRevisedFromId());
            System.out.println("Debug");
            for (StockAllocation oldAllocation : oldAllocations) {
                System.out.println("estimate: "+oldAllocation.getEstimateId());
                System.out.println("estimateItemID :"+oldAllocation.getEstimateItemId());
                System.out.println("item: "+oldAllocation.getItemId());
                System.out.println("warehouse :"+oldAllocation.getWarehouseId());

            }

            // 2. Lấy các EstimateItem của Ver 2
            List<EstimateItem> newEstimateItems = estimateItemRepository.findByEstimateId(estimateId);
            for (EstimateItem newEstimate2 : newEstimateItems) {
                System.out.println("estimate2: "+newEstimate2.getEstimateId());
                System.out.println("estimateItemID2 :"+newEstimate2.getId());
                System.out.println("item2: "+newEstimate2.getItemId());
                System.out.println("warehouse2 :"+newEstimate2.getWarehouseId());
                System.out.println("getRevisedFromItemId :"+newEstimate2.getRevisedFromItemId());

            }
            //check by estimateId compare
            Set<Integer> existingItemIds = oldAllocations.stream()
                    .map(StockAllocation::getEstimateItemId)
                    .collect(Collectors.toSet());
            List<EstimateItem> brandNewItems = newEstimateItems.stream()
                    .filter(newItem -> newItem.getRevisedFromItemId() == null
                            && newItem.getIsRemoved()==false)
                    .toList();
            for (EstimateItem createItem : brandNewItems) {
                System.out.println("estimate cr: "+createItem.getEstimateId());
                System.out.println("estimateItemID cr :"+createItem.getId());
                System.out.println("item cr: "+createItem.getItemId());
                System.out.println("warehouse cr :"+createItem.getWarehouseId());
                System.out.println("getRevisedFromItemId cr:"+createItem.getRevisedFromItemId());

            }
            for (EstimateItem newItem : brandNewItems) {
                if (newItem.getRevisedFromItemId()==null || !existingItemIds.contains(newItem.getRevisedFromItemId())) {
                System.out.println("Đây là món đồ mới tinh: " + newItem.getItemName());
                StockAllocation stockAllocation = new StockAllocation();
                stockAllocation.setServiceTicketId(newEstimate.getServiceTicketId());
                stockAllocation.setEstimateItemId(newItem.getId());
                stockAllocation.setWarehouseId(newItem.getWarehouseId());
                stockAllocation.setItemId(newItem.getItemId());
                stockAllocation.setQuantity(newItem.getQuantity());
                stockAllocation.setEstimateId(estimateId);
                stockAllocation.setStatus("RESERVED");
                stockAllocation.setCreatedBy(staffId);
                stockAllocation.setCreatedAt(Instant.now());
                StockAllocation savedStockAllocation = stockAllocationRepository.createNewAllocation(stockAllocation);
                stockAllocations.add(savedStockAllocation);
                }
            }
        }
        // =========================================================================
        // TRƯỜNG HỢP 2: TẠO BÁO GIÁ LẦN ĐẦU TIÊN (Không có Version cũ)
        // =========================================================================
        else {
            List<EstimateItem> estimateItems = estimateItemRepository.findByEstimateId(estimateId);
            for (EstimateItem estimateItem : estimateItems) {
                if (estimateItem.getItemId() != null && estimateItem.getWarehouseId() != null) {
                    StockAllocation stockAllocation = new StockAllocation();
                    stockAllocation.setServiceTicketId(newEstimate.getServiceTicketId());
                    stockAllocation.setEstimateItemId(estimateItem.getId());
                    stockAllocation.setWarehouseId(estimateItem.getWarehouseId());
                    stockAllocation.setItemId(estimateItem.getItemId());
                    stockAllocation.setQuantity(estimateItem.getQuantity());
                    stockAllocation.setEstimateId(estimateId);
                    stockAllocation.setStatus("RESERVED");
                    stockAllocation.setCreatedBy(staffId);
                    stockAllocation.setCreatedAt(Instant.now());

                    StockAllocation savedStockAllocation = stockAllocationRepository.createNewAllocation(stockAllocation);
                    stockAllocations.add(savedStockAllocation);

                    // Vì là mới tinh nên chắc chắn phải giữ kho
                    inventoryService.increaseReservedQuantity(
                            savedStockAllocation.getItemId(),
                            savedStockAllocation.getWarehouseId(),
                            savedStockAllocation.getQuantity()
                    );
                }
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
            System.out.println("DEBUG allo:"+dto.getAllocationId()+", "+dto.getStatus());
        if (dto.getAllocationId()==null) {
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
            if ("COMMITTED".equals(oldAllocation.getStatus())) {
                // Vẫn phải xóa khỏi oldMap để nó không bị lọt xuống vòng lặp Xóa ở bên dưới
                oldMap.remove(dto.getAllocationId());
                continue; // Bỏ qua mọi xử lý update bên dưới, nhảy sang dto tiếp theo
            }
            int difference = dto.getQuantity() - oldAllocation.getQuantity();
            if (difference != 0) {
                oldAllocation.setQuantity(dto.getQuantity());
                stockAllocationRepository.save(oldAllocation);

                inventoryService.updateReservedQuantityByDelta(dto.getItemId(),dto.getWarehouseId(),difference);
            }
            oldMap.remove(dto.getAllocationId());
        }
        }
        for (StockAllocation deletedAlloc : oldMap.values()) {
            if ("COMMITTED".equals(deletedAlloc.getStatus())) {
                continue;
            }
            inventoryService.decreaseReservedQuantity(deletedAlloc.getItemId(),deletedAlloc.getWarehouseId(),deletedAlloc.getQuantity());
            stockAllocationRepository.delete(deletedAlloc);
        }
        return stockAllocationRepository.findByEstimateId(estimateId).stream().map(stockAllocationDtoMapper::toDto).toList();
    }

    public List<StockAllocationDto> getStockAllocationByEstimate(Integer estimateId) {
        List<StockAllocation> stockAllocations = stockAllocationRepository.findByEstimateId(estimateId);
        return  stockAllocations.stream().map(stockAllocationDtoMapper::toDto).toList();
    }

    public List<EstimateViaAllocationDto> getEstimateToAllocation(Integer estimateId) {
        List<EstimateViaAllocationDto> stockAllocations = stockAllocationRepository.findEstimateAndAllocationById(estimateId);
        return  stockAllocations;
    }
}
