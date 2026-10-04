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
import com.g42.platform.gms.marketing.service_combo.infrastructure.entity.ComboItemJpa;
import com.g42.platform.gms.marketing.service_combo.infrastructure.repository.ComboItemRepoJpa;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.g42.platform.gms.common.util.Qty;
import java.math.BigDecimal;
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
    private final ComboItemRepoJpa comboItemRepoJpa;
    private final CatalogItemJpaRepo catalogItemJpaRepo;
    private final com.g42.platform.gms.warehouse.app.service.serial.ItemSerialService itemSerialService;

    @Transactional
    public List<StockAllocationDto> createStockAllocation(Integer estimateId, Integer staffId) {
        // Chống bấm đúp theo TỪNG DÒNG báo giá chứ không theo cả báo giá: trước đây hễ báo giá
        // có một allocation bất kỳ (kể cả RELEASED do huỷ giữ hàng) là bỏ qua toàn bộ, nên dòng
        // thêm sau khi huỷ giữ hàng không bao giờ được giữ → phiếu kẹt, không yêu cầu xuất kho
        // được mà cũng không tiến hành sửa được (MST_W4T244).
        List<StockAllocation> existingAllocations = stockAllocationRepository.findByEstimateId(estimateId);
        List<StockAllocation> activeAllocations = existingAllocations == null ? List.of() : existingAllocations.stream()
                .filter(a -> "RESERVED".equals(a.getStatus()) || "COMMITTED".equals(a.getStatus()))
                .toList();
        Set<Integer> estimateItemIdsWithActiveAllocation = activeAllocations.stream()
                .map(StockAllocation::getEstimateItemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Estimate newEstimate = estimateService.findById(estimateId);
        List<StockAllocation> stockAllocations = new ArrayList<>(activeAllocations);
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
                    .filter(newItem -> shouldAllocateItem(newItem, estimateItemIdsWithActiveAllocation))
                    .toList();
            List<EstimateItem> sortedItems = brandNewItems.stream()
                    // Dòng gõ tay / thu mua của khách không có itemId — xếp cuối thay vì NPE
                    .sorted(Comparator.comparing(EstimateItem::getItemId, Comparator.nullsLast(Comparator.naturalOrder())))
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

                    List<ComboItemJpa> comboSubItems = newItem.getItemId() != null ? comboItemRepoJpa.findAllByComboId(newItem.getItemId()) : null;
                    if (comboSubItems != null && !comboSubItems.isEmpty()) {
                        Integer warehouseId = newItem.getWarehouseId() != null ? newItem.getWarehouseId() : 1;
                        BigDecimal parentQty = newItem.getQuantity() != null ? newItem.getQuantity() : BigDecimal.ONE;

                        for (ComboItemJpa subItem : comboSubItems) {
                            if (subItem.getIncludedItemId() == null) continue;
                            CatalogItemJpa catalogItem = catalogItemJpaRepo.findById(subItem.getIncludedItemId()).orElse(null);
                            if (catalogItem != null && catalogItem.getItemType() == CatalogItemType.SERVICE) continue;

                            StockAllocation stockAllocation = new StockAllocation();
                            stockAllocation.setServiceTicketId(newEstimate.getServiceTicketId());
                            stockAllocation.setEstimateItemId(newItem.getId());
                            stockAllocation.setWarehouseId(warehouseId);
                            stockAllocation.setItemId(subItem.getIncludedItemId());
                            stockAllocation.setEntryItemId(subItem.getEntryItemId() != null ? subItem.getEntryItemId() : newItem.getEntryItemId());
                            stockAllocation.setQuantity(parentQty.multiply(BigDecimal.valueOf(subItem.getQuantity() != null ? subItem.getQuantity() : 1)));
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
                    } else if (newItem.getWarehouseId() != null) {
                        StockAllocation stockAllocation = new StockAllocation();
                        stockAllocation.setServiceTicketId(newEstimate.getServiceTicketId());
                        stockAllocation.setEstimateItemId(newItem.getId());
                        stockAllocation.setWarehouseId(newItem.getWarehouseId());
                        stockAllocation.setItemId(newItem.getItemId());
                        stockAllocation.setEntryItemId(newItem.getEntryItemId());
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
        }
        // =========================================================================
        // TRƯỜNG HỢP 2: TẠO BÁO GIÁ LẦN ĐẦU TIÊN (Không có Version cũ)
        // =========================================================================
        else {
            List<EstimateItem> estimateItems = estimateItemRepository.findByEstimateId(estimateId);
            for (EstimateItem estimateItem : estimateItems) {
                if (estimateItem.getItemId() != null && Boolean.FALSE.equals(estimateItem.getIsRemoved())
                        && shouldAllocateItem(estimateItem, estimateItemIdsWithActiveAllocation)) {
                    List<ComboItemJpa> comboSubItems = comboItemRepoJpa.findAllByComboId(estimateItem.getItemId());
                    if (comboSubItems != null && !comboSubItems.isEmpty()) {
                        Integer warehouseId = estimateItem.getWarehouseId() != null ? estimateItem.getWarehouseId() : 1;
                        BigDecimal parentQty = estimateItem.getQuantity() != null ? estimateItem.getQuantity() : BigDecimal.ONE;

                        for (ComboItemJpa subItem : comboSubItems) {
                            if (subItem.getIncludedItemId() == null) continue;
                            CatalogItemJpa catalogItem = catalogItemJpaRepo.findById(subItem.getIncludedItemId()).orElse(null);
                            if (catalogItem != null && catalogItem.getItemType() == CatalogItemType.SERVICE) continue;

                            StockAllocation stockAllocation = new StockAllocation();
                            stockAllocation.setServiceTicketId(newEstimate.getServiceTicketId());
                            stockAllocation.setEstimateItemId(estimateItem.getId());
                            stockAllocation.setWarehouseId(warehouseId);
                            stockAllocation.setItemId(subItem.getIncludedItemId());
                            stockAllocation.setEntryItemId(subItem.getEntryItemId() != null ? subItem.getEntryItemId() : estimateItem.getEntryItemId());
                            stockAllocation.setQuantity(parentQty.multiply(BigDecimal.valueOf(subItem.getQuantity() != null ? subItem.getQuantity() : 1)));
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
                    } else if (estimateItem.getWarehouseId() != null) {
                        StockAllocation stockAllocation = new StockAllocation();
                        stockAllocation.setServiceTicketId(newEstimate.getServiceTicketId());
                        stockAllocation.setEstimateItemId(estimateItem.getId());
                        stockAllocation.setWarehouseId(estimateItem.getWarehouseId());
                        stockAllocation.setItemId(estimateItem.getItemId());
                        stockAllocation.setEntryItemId(estimateItem.getEntryItemId());
                        stockAllocation.setQuantity(estimateItem.getQuantity());
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
        }

        return stockAllocations.stream().map(stockAllocationDtoMapper::toDto).toList();
    }

    /**
     * Dòng báo giá cần giữ hàng khi: chưa có allocation đang giữ/đã xuất, và không phải dòng đã
     * bỏ tick (is_checked = false — dòng đã nhả hàng, chỉ giữ lại để lưu vết, không tính tiền).
     */
    private static boolean shouldAllocateItem(EstimateItem item, Set<Integer> estimateItemIdsWithActiveAllocation) {
        if (estimateItemIdsWithActiveAllocation.contains(item.getId())) return false;
        return !Boolean.FALSE.equals(item.getIsChecked());
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
        // Khoá chống trùng là CẶP (itemId, entryItemId) chứ không phải riêng itemId:
        // một sản phẩm có thể được lấy từ nhiều lô trong cùng một báo giá (mỗi lô một
        // dòng, vì giá bán và giá vốn khác nhau theo lô). Nếu chỉ khoá theo itemId thì
        // dòng lô thứ hai bị coi là bấm đúp và bị gộp vào lô thứ nhất, hàng của lô đó
        // không được giữ.
        Map<String,StockAllocation> activeMapByItemId = oldList.stream()
                .filter(s -> s.getStatus().equals("RESERVED")||s.getStatus().equals("COMMITTED"))
                .collect(Collectors.toMap(StockAllocationService::buildItemLotKey,allocation -> allocation,
                        (existing, replacement) -> existing));
        //handle add new and update:
        for (StockAllocationDto dto : stockAllocationDtos) {
            System.out.println("DEBUG allo:"+dto.getAllocationId()+", "+dto.getStatus());
        if (dto.getAllocationId()==null) {
            //todo: check frontend duplicate
            String itemLotKey = buildItemLotKey(dto.getItemId(), dto.getEntryItemId());
            if (activeMapByItemId.containsKey(itemLotKey)) {
                System.err.println("CẢNH BÁO: Frontend gửi đúp item " + dto.getItemId() + " (lô " + dto.getEntryItemId() + ") do lỗi UI hiện lại nút Xác nhận. Tự động map về Allocation cũ.");
                StockAllocation existingAlloc = activeMapByItemId.get(itemLotKey);

                // 1. Gỡ nó khỏi oldMap để vòng lặp cuối hàm KHÔNG XÓA NHẦM nó
                oldMap.remove(existingAlloc.getAllocationId());

                // 2. Xử lý Update Delta (nếu UI có thay đổi số lượng lúc bấm xác nhận lại)
                if ("COMMITTED".equals(existingAlloc.getStatus())) {
                    continue; // Đã chốt thì không cho sửa kho nữa
                }

                BigDecimal difference = Qty.sub(dto.getQuantity(), existingAlloc.getQuantity());
                boolean entryItemIdChanged = !Objects.equals(dto.getEntryItemId(), existingAlloc.getEntryItemId());
                if (difference.signum() != 0 || entryItemIdChanged) {
                    existingAlloc.setQuantity(dto.getQuantity());
                    existingAlloc.setEntryItemId(dto.getEntryItemId());
                    stockAllocationRepository.save(existingAlloc);
                    if (difference.signum() != 0) {
                        inventoryService.updateReservedQuantityByDelta(dto.getItemId(), dto.getWarehouseId(), difference);
                    }
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
            BigDecimal difference = Qty.sub(dto.getQuantity(), oldAllocation.getQuantity());
            boolean entryItemIdChanged = !Objects.equals(dto.getEntryItemId(), oldAllocation.getEntryItemId());
            if (difference.signum() != 0 || entryItemIdChanged) {
                oldAllocation.setQuantity(dto.getQuantity());
                oldAllocation.setEntryItemId(dto.getEntryItemId());
                stockAllocationRepository.save(oldAllocation);

                if (difference.signum() != 0) {
                    inventoryService.updateReservedQuantityByDelta(dto.getItemId(),dto.getWarehouseId(),difference);
                }
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
            itemSerialService.releaseByEstimateItems(Collections.singletonList(deletedAlloc.getEstimateItemId()));
        }
        return stockAllocationRepository.findByEstimateId(estimateId).stream().map(stockAllocationDtoMapper::toDto).toList();
    }

    /** Khoá nhận dạng một dòng giữ hàng: cùng sản phẩm nhưng khác lô là hai dòng khác nhau. */
    private static String buildItemLotKey(Integer itemId, Integer entryItemId) {
        return itemId + "|" + (entryItemId == null ? "" : entryItemId);
    }

    private static String buildItemLotKey(StockAllocation allocation) {
        return buildItemLotKey(allocation.getItemId(), allocation.getEntryItemId());
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
