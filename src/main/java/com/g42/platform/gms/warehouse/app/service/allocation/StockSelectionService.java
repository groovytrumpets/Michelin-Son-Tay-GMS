package com.g42.platform.gms.warehouse.app.service.allocation;



import java.math.BigDecimal;
import com.g42.platform.gms.common.util.Qty;
import com.g42.platform.gms.warehouse.api.dto.StockSelectionConfigDto;
import com.g42.platform.gms.warehouse.api.dto.StockSuggestionDto;
import com.g42.platform.gms.warehouse.api.dto.WarehouseLotDto;
import com.g42.platform.gms.warehouse.domain.enums.StockAllocationMethod;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockSelectionConfigJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.InventoryJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockEntryItemJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.StockSelectionConfigJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.WarehouseJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Chọn kho và lô tự động khi thêm vật tư vào bảng báo giá.
 *
 * Trình tự: xác định kho (kho người dùng chỉ định → kho mặc định trong cấu hình
 * → kho còn nhiều hàng nhất nếu được phép), rồi chọn lô trong kho đó theo
 * chiến lược FIFO / LIFO / FEFO. Chiến lược MANUAL thì chỉ trả về kho và để
 * người dùng tự bấm chọn lô.
 */
@Service
@RequiredArgsConstructor
public class StockSelectionService {

    private final StockSelectionConfigJpaRepo stockSelectionConfigJpaRepo;
    private final StockEntryItemJpaRepo stockEntryItemJpaRepo;
    private final InventoryJpaRepo inventoryJpaRepo;
    private final WarehouseJpaRepo warehouseJpaRepo;

    // ===== Cấu hình =====

    /** Cấu hình đang có hiệu lực; chưa có thì trả về mặc định FIFO. */
    public StockSelectionConfigJpa getActiveConfig() {
        return stockSelectionConfigJpaRepo.findFirstByIsActiveTrueOrderByConfigIdDesc()
                .orElseGet(StockSelectionConfigJpa::new);
    }

    public StockSelectionConfigDto getConfigDto() {
        return toConfigDto(getActiveConfig());
    }

    @Transactional
    public StockSelectionConfigDto saveConfig(StockSelectionConfigDto request, Integer staffId) {
        StockSelectionConfigJpa config = stockSelectionConfigJpaRepo
                .findFirstByIsActiveTrueOrderByConfigIdDesc()
                .orElseGet(StockSelectionConfigJpa::new);

        config.setDefaultWarehouseId(request.getDefaultWarehouseId());
        if (request.getAllocationMethod() != null) {
            config.setAllocationMethod(request.getAllocationMethod());
        }
        if (request.getFallbackToAnyWarehouse() != null) {
            config.setFallbackToAnyWarehouse(request.getFallbackToAnyWarehouse());
        }
        config.setIsActive(true);
        config.setUpdatedBy(staffId);
        config.setUpdatedAt(Instant.now());

        return toConfigDto(stockSelectionConfigJpaRepo.save(config));
    }

    // ===== Gợi ý kho + lô =====

    /**
     * @param itemId      vật tư cần xuất
     * @param quantity    số lượng dự kiến, dùng để ưu tiên lô còn đủ hàng
     * @param warehouseId kho người dùng đã chỉ định; để trống thì hệ thống tự chọn
     */
    public StockSuggestionDto suggest(Integer itemId, BigDecimal quantity, Integer warehouseId) {
        StockSelectionConfigJpa config = getActiveConfig();
        StockAllocationMethod method = config.getAllocationMethod() == null
                ? StockAllocationMethod.FIFO
                : config.getAllocationMethod();

        StockSuggestionDto result = new StockSuggestionDto();
        result.setItemId(itemId);
        result.setAllocationMethod(method);

        Integer resolvedWarehouseId = resolveWarehouseId(itemId, warehouseId, config);
        if (resolvedWarehouseId == null) {
            result.setMessage("Vật tư này chưa có tồn ở kho nào.");
            return result;
        }

        result.setWarehouseId(resolvedWarehouseId);
        warehouseJpaRepo.findById(resolvedWarehouseId)
                .map(WarehouseJpa::getWarehouseName)
                .ifPresent(result::setWarehouseName);

        inventoryJpaRepo.findByWarehouseIdAndItemId(resolvedWarehouseId, itemId)
                .ifPresent(inv -> result.setAvailableQuantity(availableOf(inv)));

        if (method == StockAllocationMethod.MANUAL) {
            result.setMessage("Cấu hình đang để chọn lô thủ công.");
            return result;
        }

        List<WarehouseLotDto> lots = stockEntryItemJpaRepo.findWarehouseLots(resolvedWarehouseId, itemId);
        if (lots == null || lots.isEmpty()) {
            result.setMessage("Kho đã chọn không còn lô nào của vật tư này.");
            return result;
        }

        WarehouseLotDto picked = pickLot(lots, method, quantity);
        result.setEntryItemId(picked.getEntryItemId());
        result.setEntryCode(picked.getEntryCode());
        result.setImportPrice(picked.getImportPrice());
        result.setEntryDate(picked.getEntryDate());
        result.setExpiryDate(picked.getExpiryDate());
        return result;
    }

    /**
     * Sắp xếp lô theo chiến lược rồi ưu tiên lô đầu tiên còn đủ số lượng cần.
     * Không lô nào đủ thì lấy lô đứng đầu — phần thiếu sẽ do luồng xuất kho
     * chia tiếp sang các lô sau.
     */
    private WarehouseLotDto pickLot(List<WarehouseLotDto> lots, StockAllocationMethod method, BigDecimal quantity) {
        List<WarehouseLotDto> sorted = lots.stream().sorted(comparatorFor(method)).toList();
        BigDecimal needed = quantity == null || quantity.signum() <= 0 ? BigDecimal.ONE : quantity;

        return sorted.stream()
                .filter(lot -> lot.getRemainingQuantity() != null && !Qty.lt(lot.getRemainingQuantity(), needed))
                .findFirst()
                .orElse(sorted.get(0));
    }

    private Comparator<WarehouseLotDto> comparatorFor(StockAllocationMethod method) {
        Comparator<WarehouseLotDto> byEntryDate =
                Comparator.comparing(WarehouseLotDto::getEntryDate, Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<WarehouseLotDto> byEntryItemId =
                Comparator.comparing(WarehouseLotDto::getEntryItemId, Comparator.nullsLast(Comparator.naturalOrder()));

        return switch (method) {
            // Nhập sau xuất trước
            case LIFO -> byEntryDate.reversed().thenComparing(byEntryItemId.reversed());
            // Hết hạn trước xuất trước; lô không có hạn xếp sau cùng
            case FEFO -> Comparator
                    .comparing(WarehouseLotDto::getExpiryDate, Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()))
                    .thenComparing(byEntryDate)
                    .thenComparing(byEntryItemId);
            // FIFO và mọi trường hợp còn lại: nhập trước xuất trước
            default -> byEntryDate.thenComparing(byEntryItemId);
        };
    }

    private Integer resolveWarehouseId(Integer itemId, Integer requestedWarehouseId, StockSelectionConfigJpa config) {
        if (requestedWarehouseId != null && requestedWarehouseId > 0) return requestedWarehouseId;

        Integer defaultWarehouseId = config.getDefaultWarehouseId();
        if (defaultWarehouseId != null) {
            Optional<InventoryJpa> inv = inventoryJpaRepo.findByWarehouseIdAndItemId(defaultWarehouseId, itemId);
            if (inv.isPresent() && Qty.isPositive(availableOf(inv.get()))) return defaultWarehouseId;
            // Kho mặc định hết hàng mà không cho tìm kho khác thì vẫn trả kho mặc định,
            // để người dùng thấy đúng kho đã cấu hình và tự xử lý.
            if (!Boolean.TRUE.equals(config.getFallbackToAnyWarehouse())) return defaultWarehouseId;
        }

        return inventoryJpaRepo.findByItemIdOrderByQuantityDesc(itemId).stream()
                .filter(inv -> Qty.isPositive(availableOf(inv)))
                .map(InventoryJpa::getWarehouseId)
                .findFirst()
                .orElse(defaultWarehouseId);
    }

    /** Tồn khả dụng = tồn kho trừ phần đang được giữ cho các phiếu khác. */
    private BigDecimal availableOf(InventoryJpa inventory) {
        return Qty.subFloorZero(inventory.getQuantity(), inventory.getReservedQuantity());
    }

    private StockSelectionConfigDto toConfigDto(StockSelectionConfigJpa config) {
        String warehouseName = config.getDefaultWarehouseId() == null ? null
                : warehouseJpaRepo.findById(config.getDefaultWarehouseId())
                        .map(WarehouseJpa::getWarehouseName)
                        .orElse(null);

        return new StockSelectionConfigDto(
                config.getConfigId(),
                config.getDefaultWarehouseId(),
                warehouseName,
                config.getAllocationMethod() == null ? StockAllocationMethod.FIFO : config.getAllocationMethod(),
                config.getFallbackToAnyWarehouse(),
                config.getUpdatedAt());
    }
}
