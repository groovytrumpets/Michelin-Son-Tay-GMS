package com.g42.platform.gms.estimation.infrastructure;

import com.g42.platform.gms.estimation.api.dto.EstimateViaAllocationDto;
import com.g42.platform.gms.estimation.api.mapper.EstimateDtoMapper;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.entity.StockAllocation;
import com.g42.platform.gms.estimation.domain.repository.StockAllocationRepository;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateItemJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateJpa;
import com.g42.platform.gms.estimation.infrastructure.entity.StockAllocationJpa;
import com.g42.platform.gms.estimation.infrastructure.mapper.StockAllocationJpaMapper;
import com.g42.platform.gms.estimation.infrastructure.repository.EstimateRepositoryJpa;
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
    private final EstimateRepositoryJpa estimateRepositoryJpa;

    public StockAllocationRepoImpl(StockAllocationJpaMapper stockAllocationJpaMapper, StockAllocationRepositoryJpa stockAllocationRepositoryJpa, EstimateItemJpaRepository estimateItemJpaRepository, EstimateDtoMapper estimateDtoMapper, EstimateRepositoryJpa estimateRepositoryJpa) {
        this.stockAllocationJpaMapper = stockAllocationJpaMapper;
        this.stockAllocationRepositoryJpa = stockAllocationRepositoryJpa;
        this.estimateItemJpaRepository = estimateItemJpaRepository;
        this.estimateDtoMapper = estimateDtoMapper;
        this.estimateRepositoryJpa = estimateRepositoryJpa;
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

    @Override
    public List<EstimateViaAllocationDto> findEstimateAndAllocationById(Integer estimateId) {
        // 1. Lấy thông tin Estimate hiện tại để lấy được serviceTicketId
        EstimateJpa currentEstimate = estimateRepositoryJpa.findById(estimateId).orElseThrow();

        // 2. Lấy TOÀN BỘ EstimateItem của TICKET NÀY (để vòng lặp while không bị gãy đoạn)
        List<EstimateItemJpa> allItemsInTicket = estimateItemJpaRepository.findByServiceTicketId(currentEstimate.getServiceTicketId());

        // 3. Lấy TOÀN BỘ StockAllocation của TICKET NÀY (Tầm nhìn bao quát cả V1, V2, V3...)
        List<StockAllocationJpa> allAllocationsInTicket = stockAllocationRepositoryJpa.findAllByServiceTicketId(currentEstimate.getServiceTicketId());

        // 4. Lọc ra danh sách item CHỈ CỦA VERSION HIỆN TẠI để hiển thị
        List<EstimateItemJpa> activeItemsInCurrentVersion = allItemsInTicket.stream()
//                .filter(i -> i.getEstimateId().equals(estimateId)) // Chỉ lấy của version này
                .filter(i -> Boolean.FALSE.equals(i.getIsRemoved())) // Chỉ lấy item chưa bị xóa
                .toList();

        // 5. Xây dựng 2 Map tra cứu TOÀN CỤC (Global Lookup Maps)
        Map<Integer, StockAllocationJpa> globalAllocationMap = allAllocationsInTicket.stream()
                .collect(Collectors.toMap(
                        StockAllocationJpa::getEstimateItemId,
                        a -> a,
                        (a1, a2) -> a1 // Lấy bản đầu tiên nếu trùng
                ));

        Map<Integer, EstimateItemJpa> globalItemMap = allItemsInTicket.stream()
                .collect(Collectors.toMap(EstimateItemJpa::getId, i -> i));

        // 6. Map dữ liệu
        return activeItemsInCurrentVersion.stream().map(activeItem -> {
            EstimateViaAllocationDto dto = new EstimateViaAllocationDto();
            dto.setEstimateItemDto(estimateDtoMapper.toEstimateItemDtoJpa(activeItem));

            // Sử dụng Map Toàn cục để Resolve
            StockAllocationJpa allocation = resolveAllocation(
                    activeItem,
                    globalAllocationMap, // Truyền Map toàn cục vào
                    globalItemMap        // Truyền Map toàn cục vào
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
