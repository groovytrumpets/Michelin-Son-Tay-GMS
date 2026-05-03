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
        List<StockAllocation> existingAllocations = stockAllocationRepository.findByEstimateId(estimateId);
        if (existingAllocations!=null && !existingAllocations.isEmpty()) {
            System.err.println("Estimate ID " + estimateId + " đã có allocation, bỏ qua tạo mới.");
            return existingAllocations.stream().map(stockAllocationDtoMapper::toDto).toList();
        }

        Estimate newEstimate = estimateService.findById(estimateId);
        List<StockAllocation> stockAllocations = new ArrayList<>();
        Map<Integer, Integer> itemAncestryMap = new HashMap<>();
        Integer currentRevIdToCheck = newEstimate.getRevisedFromId();

        // =========================================================================
        // TRƯỜNG HỢP 1: TẠO VERSION MỚI TỪ VERSION CŨ (REVISION)
        // =========================================================================
        if (newEstimate.getRevisedFromId() != null) {
            List<StockAllocation> oldAllocations = new ArrayList<>();
            while (currentRevIdToCheck !=null){
                List<StockAllocation> oldAllocations2 = stockAllocationRepository.findByEstimateId(currentRevIdToCheck);
                oldAllocations.addAll(oldAllocations2);
                List<EstimateItem> oldItems = estimateItemRepository.findByEstimateId(currentRevIdToCheck);
                for (EstimateItem oldItem : oldItems) {
                    itemAncestryMap.put(oldItem.getId(),oldItem.getRevisedFromItemId());
                }
                Estimate prevEstimate = estimateService.findById(currentRevIdToCheck);
                currentRevIdToCheck = prevEstimate.getRevisedFromId();
            }

            // 1. Lấy toàn bộ Allocation cũ của Ver 1 (Cả COMMITTED)

            List<EstimateItem> oldEstimateItems = estimateItemRepository.findByEstimateId(newEstimate.getRevisedFromId());

            // 2. Lấy các EstimateItem của Ver 2
            List<EstimateItem> newEstimateItems = estimateItemRepository.findByEstimateId(estimateId);

            //check by estimateId compare
            Set<Integer> existingItemIds = oldAllocations.stream()
                    .map(StockAllocation::getEstimateItemId)
                    .collect(Collectors.toSet());
            List<EstimateItem> brandNewItems = newEstimateItems.stream()
                    .filter(newItem -> newItem.getIsRemoved()==false)
                    .toList();
            List<EstimateItem> sortedItems = brandNewItems.stream()
                    .sorted(Comparator.comparing(EstimateItem::getItemId))
                    .toList();
            for (EstimateItem newItem : sortedItems) {
                boolean hasAllocationInChain = false;
                Integer ancestorId = newItem.getRevisedFromItemId();

                while (ancestorId!=null){
                    if (existingItemIds.contains(ancestorId)){
                        hasAllocationInChain = true;
                        break;
                    }
                    ancestorId = itemAncestryMap.get(ancestorId);
                }

                if (!hasAllocationInChain) {
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

                    inventoryService.increaseReservedQuantity(
                            savedStockAllocation.getItemId(),
                            savedStockAllocation.getWarehouseId(),
                            savedStockAllocation.getQuantity()
                    );
                }
            }
        }
        // =========================================================================
        // TRƯỜNG HỢP 2: TẠO BÁO GIÁ LẦN ĐẦU TIÊN (Không có Version cũ)
        // =========================================================================
        else {
            List<EstimateItem> estimateItems = estimateItemRepository.findByEstimateId(estimateId);
            for (EstimateItem estimateItem : estimateItems) {
                if (estimateItem.getItemId() != null && estimateItem.getWarehouseId() != null && estimateItem.getIsRemoved()==false) {
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
        if (oldMap.isEmpty()){
            return createStockAllocation(estimateId, staffId);
        }
        Map<Integer,StockAllocation> activeMapByItemId = oldList.stream()
                .filter(s -> s.getStatus().equals("RESERVED")||s.getStatus().equals("COMMITTED"))
                .collect(Collectors.toMap(StockAllocation::getItemId,allocation -> allocation,
                        (existing, replacement) -> existing));
        //handle add new and update:
        for (StockAllocationDto dto : stockAllocationDtos) {
            System.out.println("DEBUG allo:"+dto.getAllocationId()+", "+dto.getStatus());
        if (dto.getAllocationId()==null) {
            //todo: check frontend duplicate
            if (activeMapByItemId.containsKey(dto.getItemId())) {
                System.err.println("CẢNH BÁO: Frontend gửi đúp item " + dto.getItemId() + " (do lỗi UI hiện lại nút Xác nhận). Tự động map về Allocation cũ.");
                StockAllocation existingAlloc = activeMapByItemId.get(dto.getItemId());

                // 1. Gỡ nó khỏi oldMap để vòng lặp cuối hàm KHÔNG XÓA NHẦM nó
                oldMap.remove(existingAlloc.getAllocationId());

                // 2. Xử lý Update Delta (nếu UI có thay đổi số lượng lúc bấm xác nhận lại)
                if ("COMMITTED".equals(existingAlloc.getStatus())) {
                    continue; // Đã chốt thì không cho sửa kho nữa
                }

                int difference = dto.getQuantity() - existingAlloc.getQuantity();
                if (difference != 0) {
                    existingAlloc.setQuantity(dto.getQuantity());
                    stockAllocationRepository.save(existingAlloc);
                    inventoryService.updateReservedQuantityByDelta(dto.getItemId(), dto.getWarehouseId(), difference);
                }

                continue; // Xử lý xong, BỎ QUA lệnh add new bên dưới
            }
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
