package com.g42.platform.gms.warehouse.app.service.inventory;

import com.g42.platform.gms.estimation.domain.exception.EstimateErrorCode;
import com.g42.platform.gms.estimation.domain.exception.EstimateException;
import com.g42.platform.gms.warehouse.api.dto.response.InventoryResponse;
import com.g42.platform.gms.warehouse.app.service.dto.StockRequest;
import com.g42.platform.gms.warehouse.app.service.dto.StockShortageInfo;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.WarehousePricing;
import com.g42.platform.gms.warehouse.domain.enums.CatalogItemType;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.PartCatalogRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import com.g42.platform.gms.warehouse.domain.repository.WarehousePricingRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepo inventoryRepo;
    private final PartCatalogRepo partCatalogRepo;
    private final StockEntryRepo stockEntryRepo;
    private final WarehousePricingRepo pricingRepo;
    @Transactional(readOnly = true)
    public int getAvailableQuantity(Integer warehouseId, Integer itemId) {
        return inventoryRepo.findByWarehouseAndItem(warehouseId, itemId)
                .map(Inventory::getAvailableQuantity)
                .orElse(0);    }

    /** Tăng reserved_quantity — dùng khi tạo stock allocation */
    @Transactional
    public void increaseReservedQuantity(Integer itemId, Integer warehouseId, Integer quantity) {

        Inventory inv = inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId)
                .orElseThrow(() -> new EstimateException("Không tìm thấy thông tin tồn kho cho sản phẩm này!", EstimateErrorCode.BAD_REQUEST));
        if (inv.getAvailableQuantity() < quantity) {
            throw new EstimateException(
                    "Sản phẩm không đủ tồn kho để giữ chỗ! Khả dụng: " + inv.getAvailableQuantity() + ", Yêu cầu: " + quantity,
                    EstimateErrorCode.OUT_OF_STOCK
            );
        }
        int newReserved = (inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0) + quantity;
        inv.setReservedQuantity(newReserved);

        inventoryRepo.save(inv);

//        inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId).ifPresent(inv -> {
//            int newReserved = (inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0) + quantity;
//            inv.setReservedQuantity(Math.max(0, newReserved));
//            inventoryRepo.save(inv);
//        });
    }

    /** Giảm reserved_quantity — dùng khi release stock allocation */
    @Transactional
    public void decreaseReservedQuantity(Integer itemId, Integer warehouseId, Integer quantity) {
        inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId).ifPresent(inv -> {
            int newReserved = (inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0) - quantity;
            inv.setReservedQuantity(Math.max(0, newReserved));
            inventoryRepo.save(inv);
        });
    }

    /** Cập nhật reserved_quantity theo delta (dương = tăng, âm = giảm) */
    @Transactional
    public void updateReservedQuantityByDelta(Integer itemId, Integer warehouseId, int delta) {
        inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId).ifPresent(inv -> {
            int newReserved = (inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0) + delta;
            inv.setReservedQuantity(Math.max(0, newReserved));
            inventoryRepo.save(inv);
        });
    }

    /** Cập nhật inventory khi estimate được duyệt (trừ quantity thực tế) */
    @Transactional
    public void updateInventoryByEstimate(Integer itemId, Integer warehouseId, Integer quantity) {
        inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId).ifPresent(inv -> {
            int newQty = (inv.getQuantity() != null ? inv.getQuantity() : 0) - quantity;
            int newReserved = (inv.getReservedQuantity() != null ? inv.getReservedQuantity() : 0) - quantity;
            inv.setQuantity(Math.max(0, newQty));
            inv.setReservedQuantity(Math.max(0, newReserved));
            inventoryRepo.save(inv);
        });
    }
    @Transactional(readOnly = true)
    public List<StockShortageInfo> checkAvailability(List<StockRequest> requests) {
        List<StockShortageInfo> shortages = new ArrayList<>();
        for (StockRequest req : requests) {
            int available = getAvailableQuantity(req.getWarehouseId(), req.getItemId());
            if (available < req.getQuantity()) {
                shortages.add(new StockShortageInfo(
                        req.getWarehouseId(), req.getItemId(), req.getQuantity(), available));
            }
        }
        return shortages;
    }
    @Transactional(readOnly = true)
    public List<InventoryResponse> listByWarehouse(Integer warehouseId,
                                                   boolean showImportPrice,
                                                   boolean showSellingPrice) {
        List<Inventory> invList = inventoryRepo.findByWarehouse(warehouseId);

        List<Integer> itemIds = invList.stream().map(Inventory::getItemId).collect(Collectors.toList());
        // Chỉ lấy catalog item có type = PART
        Map<Integer, CatalogItem> catalogMap = partCatalogRepo.findAllPartsByIds(itemIds).stream()
                .filter(c -> c.getItemType() == CatalogItemType.PART)
            .collect(Collectors.toMap(CatalogItem::getItemId, c -> c));

        return invList.stream()
                .filter(inv -> catalogMap.containsKey(inv.getItemId()))
                .map(inv -> {
                    InventoryResponse r = new InventoryResponse();
                    r.setInventoryId(inv.getInventoryId());
                    r.setWarehouseId(inv.getWarehouseId());
                    r.setItemId(inv.getItemId());
                    r.setQuantity(inv.getQuantity());
                    r.setReservedQuantity(inv.getReservedQuantity());
                    r.setAvailableQuantity(inv.getAvailableQuantity());

                    CatalogItem catalog = catalogMap.get(inv.getItemId());
                    r.setItemName(catalog.getItemName());
                    r.setSku(catalog.getSku());
                    r.setUnit(catalog.getUnit());

                    if (showSellingPrice) {
                        BigDecimal selling = pricingRepo
                                .findActiveByWarehouseAndItem(warehouseId, inv.getItemId())
                            .map(WarehousePricing::getSellingPrice)
                                .orElse(null);
                        if (selling == null) {
                            selling = stockEntryRepo.findFifoLots(warehouseId, inv.getItemId()).stream()
                                    .findFirst()
                                    .map(lot -> lot.getImportPrice()
                                            .multiply(lot.getMarkupMultiplier())
                                            .setScale(2, RoundingMode.HALF_UP))
                                    .orElse(null);
                        }
                        r.setSellingPrice(selling);
                    }

                    if (showImportPrice) {
                        stockEntryRepo.findFifoLots(warehouseId, inv.getItemId()).stream()
                                .findFirst()
                                .ifPresent(lot -> r.setImportPrice(lot.getImportPrice()));
                    }

                    return r;
                }).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<InventoryResponse> listAllPartsWithInventory(Integer warehouseId, boolean showImportPrice) {
        return buildInventoryResponseFromCatalog(warehouseId, partCatalogRepo.findAllParts(), showImportPrice);
    }
    @Transactional(readOnly = true)
    public List<InventoryResponse> searchByWarehouse(Integer warehouseId, String keyword,
                                                     boolean showImportPrice) {
        return buildInventoryResponseFromCatalog(warehouseId, partCatalogRepo.searchParts(keyword), showImportPrice);
    }

    private List<InventoryResponse> buildInventoryResponseFromCatalog(
            Integer warehouseId, List<CatalogItem> catalogs, boolean showImportPrice) {
        return catalogs.stream().map(catalog -> {
            InventoryResponse r = new InventoryResponse();
            r.setItemId(catalog.getItemId());
            r.setWarehouseId(warehouseId);
            r.setItemName(catalog.getItemName());
            r.setSku(catalog.getSku());
            r.setUnit(catalog.getUnit());

            inventoryRepo.findByWarehouseAndItem(warehouseId, catalog.getItemId())
                    .ifPresentOrElse(inv -> {
                        r.setInventoryId(inv.getInventoryId());
                        r.setQuantity(inv.getQuantity());
                        r.setReservedQuantity(inv.getReservedQuantity());
                        r.setAvailableQuantity(Math.max(0, inv.getQuantity() - inv.getReservedQuantity()));
                    }, () -> {
                        r.setQuantity(0);
                        r.setReservedQuantity(0);
                        r.setAvailableQuantity(0);
                    });

            if (showImportPrice) {
                // Lấy giá nhập lần gần nhất để tham khảo khi nhập kho mới
                stockEntryRepo.findLatestLot(warehouseId, catalog.getItemId())
                        .ifPresent(lot -> r.setImportPrice(lot.getImportPrice()));
            }
            return r;
        }).collect(Collectors.toList());
    }
}
