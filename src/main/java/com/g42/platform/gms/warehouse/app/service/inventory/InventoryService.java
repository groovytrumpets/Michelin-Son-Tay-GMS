package com.g42.platform.gms.warehouse.app.service.inventory;


import com.g42.platform.gms.common.util.Qty;
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
import com.g42.platform.gms.warehouse.domain.repository.WarehouseRepo;
import com.g42.platform.gms.warehouse.app.service.catalog.WarehouseHierarchy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepo inventoryRepo;
    private final PartCatalogRepo partCatalogRepo;
    private final StockEntryRepo stockEntryRepo;
    private final WarehousePricingRepo pricingRepo;
    private final WarehouseRepo warehouseRepo;
    @Transactional(readOnly = true)
    public BigDecimal getAvailableQuantity(Integer warehouseId, Integer itemId) {
        return inventoryRepo.findByWarehouseAndItem(warehouseId, itemId)
                .map(Inventory::getAvailableQuantity)
                .orElse(BigDecimal.ZERO);
    }

    /** Tăng reserved_quantity — dùng khi tạo stock allocation */
    @Transactional
    public void increaseReservedQuantity(Integer itemId, Integer warehouseId, BigDecimal quantity) {

        Inventory inv = inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId)
                .orElseThrow(() -> new EstimateException("Không tìm thấy thông tin tồn kho cho sản phẩm này!", EstimateErrorCode.BAD_REQUEST));
        if (Qty.lt(inv.getAvailableQuantity(), quantity)) {
            throw new EstimateException(
                    "Sản phẩm không đủ tồn kho để giữ chỗ! Khả dụng: " + Qty.text(inv.getAvailableQuantity()) + ", Yêu cầu: " + Qty.text(quantity),
                    EstimateErrorCode.OUT_OF_STOCK
            );
        }
        BigDecimal newReserved = Qty.add(inv.getReservedQuantity(), quantity);
        inv.setReservedQuantity(newReserved);

        inventoryRepo.save(inv);

//        inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId).ifPresent(inv -> {
//            BigDecimal newReserved = Qty.add(inv.getReservedQuantity(), quantity);
//            inv.setReservedQuantity(newReserved.max(BigDecimal.ZERO));
//            inventoryRepo.save(inv);
//        });
    }

    /** Giảm reserved_quantity — dùng khi release stock allocation */
    @Transactional
    public void decreaseReservedQuantity(Integer itemId, Integer warehouseId, BigDecimal quantity) {
        inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId).ifPresent(inv -> {
            BigDecimal newReserved = Qty.sub(inv.getReservedQuantity(), quantity);
            inv.setReservedQuantity(newReserved.max(BigDecimal.ZERO));
            inventoryRepo.save(inv);
        });
    }

    /** Cập nhật reserved_quantity theo delta (dương = tăng, âm = giảm) */
    @Transactional
    public void updateReservedQuantityByDelta(Integer itemId, Integer warehouseId, BigDecimal delta) {
        inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId).ifPresent(inv -> {
            BigDecimal newReserved = Qty.add(inv.getReservedQuantity(), delta);
            inv.setReservedQuantity(newReserved.max(BigDecimal.ZERO));
            inventoryRepo.save(inv);
        });
    }

    /** Cập nhật inventory khi estimate được duyệt (trừ quantity thực tế) */
    @Transactional
    public void updateInventoryByEstimate(Integer itemId, Integer warehouseId, BigDecimal quantity) {
        inventoryRepo.findByWarehouseAndItemWithLock(warehouseId, itemId).ifPresent(inv -> {
            BigDecimal newQty = Qty.sub(inv.getQuantity(), quantity);
            BigDecimal newReserved = Qty.sub(inv.getReservedQuantity(), quantity);
            inv.setQuantity(newQty.max(BigDecimal.ZERO));
            inv.setReservedQuantity(newReserved.max(BigDecimal.ZERO));
            inventoryRepo.save(inv);
        });
    }
    @Transactional(readOnly = true)
    public List<StockShortageInfo> checkAvailability(List<StockRequest> requests) {
        List<StockShortageInfo> shortages = new ArrayList<>();
        for (StockRequest req : requests) {
            BigDecimal available = getAvailableQuantity(req.getWarehouseId(), req.getItemId());
            if (Qty.lt(available, req.getQuantity())) {
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

                    fillPrices(r, warehouseId, inv.getItemId(), showImportPrice, showSellingPrice);
                    return r;
                }).collect(Collectors.toList());
    }

    /**
     * Tồn kho đa cấp: kho đang xem + mọi kho con cháu (bỏ nhánh kho lỗi), cộng dồn theo mặt hàng.
     * quantity/reserved/available là số tổng, ownQuantity là phần nằm trực tiếp trong kho đang xem.
     * Xuất/giữ hàng vẫn làm trên kho vật lý cụ thể — số tổng này chỉ để xem.
     */
    @Transactional(readOnly = true)
    public List<InventoryResponse> listByWarehouseTree(Integer warehouseId,
                                                       boolean showImportPrice,
                                                       boolean showSellingPrice) {
        Set<Integer> scope = new WarehouseHierarchy(warehouseRepo.findAllWarehouses())
                .rollupWarehouseIds(warehouseId);

        Map<Integer, List<Inventory>> rowsByItem = new LinkedHashMap<>();
        for (Integer wid : scope) {
            for (Inventory inv : inventoryRepo.findByWarehouse(wid)) {
                rowsByItem.computeIfAbsent(inv.getItemId(), k -> new ArrayList<>()).add(inv);
            }
        }

        Map<Integer, CatalogItem> catalogMap = partCatalogRepo.findAllPartsByIds(new ArrayList<>(rowsByItem.keySet())).stream()
                .filter(c -> c.getItemType() == CatalogItemType.PART)
                .collect(Collectors.toMap(CatalogItem::getItemId, c -> c));

        List<InventoryResponse> result = new ArrayList<>();
        rowsByItem.forEach((itemId, rows) -> {
            CatalogItem catalog = catalogMap.get(itemId);
            if (catalog == null) return;
            Inventory own = rows.stream()
                    .filter(inv -> warehouseId.equals(inv.getWarehouseId()))
                    .findFirst().orElse(null);

            InventoryResponse r = new InventoryResponse();
            r.setInventoryId(own != null ? own.getInventoryId() : null);
            r.setWarehouseId(warehouseId);
            r.setItemId(itemId);
            r.setItemName(catalog.getItemName());
            r.setSku(catalog.getSku());
            r.setUnit(catalog.getUnit());
            r.setOwnQuantity(own != null ? Qty.nz(own.getQuantity()) : BigDecimal.ZERO);
            BigDecimal qty = BigDecimal.ZERO, reserved = BigDecimal.ZERO, available = BigDecimal.ZERO;
            for (Inventory inv : rows) {
                qty = qty.add(Qty.nz(inv.getQuantity()));
                reserved = reserved.add(Qty.nz(inv.getReservedQuantity()));
                // Khả dụng tính theo từng kho rồi mới cộng: hàng giữ ở kho này không được bù bằng hàng kho khác
                available = available.add(inv.getAvailableQuantity());
            }
            r.setQuantity(qty);
            r.setReservedQuantity(reserved);
            r.setAvailableQuantity(available);

            // Giá theo kho đang xem; kho cha không chứa trực tiếp mặt hàng thì lấy theo kho con đầu tiên có hàng
            Integer priceWarehouseId = own != null ? warehouseId : rows.get(0).getWarehouseId();
            fillPrices(r, priceWarehouseId, itemId, showImportPrice, showSellingPrice);
            result.add(r);
        });
        return result;
    }

    private void fillPrices(InventoryResponse r, Integer warehouseId, Integer itemId,
                            boolean showImportPrice, boolean showSellingPrice) {
        if (showSellingPrice) {
            BigDecimal selling = pricingRepo
                    .findActiveByWarehouseAndItem(warehouseId, itemId)
                    .map(WarehousePricing::getSellingPrice)
                    .orElse(null);
            if (selling == null) {
                selling = stockEntryRepo.findFifoLots(warehouseId, itemId).stream()
                        .findFirst()
                        .map(lot -> lot.getImportPrice()
                                .multiply(lot.getMarkupMultiplier())
                                .setScale(2, RoundingMode.HALF_UP))
                        .orElse(null);
            }
            r.setSellingPrice(selling);
        }

        if (showImportPrice) {
            stockEntryRepo.findFifoLots(warehouseId, itemId).stream()
                    .findFirst()
                    .ifPresent(lot -> r.setImportPrice(lot.getImportPrice()));
        }
    }

    /**
     * Tổng hợp tồn kho từ TẤT CẢ kho — dùng cho xuất Excel toàn hệ thống.
     * Số lượng được cộng dồn theo itemId từ tất cả các kho.
     * Giá bán/giá nhập không được tổng hợp vì mỗi kho có thể có giá khác nhau.
     */
    @Transactional(readOnly = true)
    public List<InventoryResponse> listAllWarehouses() {
        List<Inventory> allInventory = inventoryRepo.findAll();

        // Group by itemId → tính tổng quantity và reservedQuantity
        Map<Integer, BigDecimal> totalQtyByItem = new java.util.LinkedHashMap<>();
        Map<Integer, BigDecimal> totalReservedByItem = new java.util.LinkedHashMap<>();
        for (Inventory inv : allInventory) {
            totalQtyByItem.merge(inv.getItemId(), Qty.nz(inv.getQuantity()), BigDecimal::add);
            totalReservedByItem.merge(inv.getItemId(), Qty.nz(inv.getReservedQuantity()), BigDecimal::add);
        }

        // Lấy tất cả catalog items (Part, Service, v.v)
        List<CatalogItem> allItems = partCatalogRepo.findAllItems();

        List<InventoryResponse> result = new java.util.ArrayList<>();
        for (CatalogItem catalog : allItems) {
            BigDecimal qty = totalQtyByItem.getOrDefault(catalog.getItemId(), BigDecimal.ZERO);
            BigDecimal reserved = totalReservedByItem.getOrDefault(catalog.getItemId(), BigDecimal.ZERO);

            InventoryResponse r = new InventoryResponse();
            r.setItemId(catalog.getItemId());
            r.setItemName(catalog.getItemName());
            r.setSku(catalog.getSku());
            r.setUnit(catalog.getUnit());
            r.setQuantity(qty);
            r.setReservedQuantity(reserved);
            r.setAvailableQuantity(Qty.subFloorZero(qty, reserved));
            result.add(r);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> listAllPartsWithInventory(Integer warehouseId, boolean showImportPrice) {
        return buildInventoryResponseFromCatalog(warehouseId, partCatalogRepo.findAllItems(), showImportPrice);
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
                        r.setAvailableQuantity(Qty.subFloorZero(inv.getQuantity(), inv.getReservedQuantity()));
                    }, () -> {
                        r.setQuantity(BigDecimal.ZERO);
                        r.setReservedQuantity(BigDecimal.ZERO);
                        r.setAvailableQuantity(BigDecimal.ZERO);
                    });

            if (showImportPrice) {
                // Lấy giá nhập lần gần nhất để tham khảo khi nhập kho mới
                stockEntryRepo.findLatestLot(warehouseId, catalog.getItemId())
                        .ifPresent(lot -> r.setImportPrice(lot.getImportPrice()));
            }
            return r;
        }).collect(Collectors.toList());
    }

    /**
     * Bổ sung sellingPrice vào danh sách InventoryResponse đã build.
     * Ưu tiên: WarehousePricing → FIFO lot importPrice * markup.
     * Dùng khi cần tách sellingPrice khỏi buildInventoryResponseFromCatalog.
     */
    @Transactional(readOnly = true)
    public List<InventoryResponse> enrichSellingPrice(List<InventoryResponse> list, Integer warehouseId) {
        list.forEach(r -> {
            BigDecimal selling = pricingRepo
                    .findActiveByWarehouseAndItem(warehouseId, r.getItemId())
                    .map(WarehousePricing::getSellingPrice)
                    .orElse(null);
            if (selling == null) {
                selling = stockEntryRepo.findFifoLots(warehouseId, r.getItemId()).stream()
                        .findFirst()
                        .map(lot -> lot.getImportPrice()
                                .multiply(lot.getMarkupMultiplier())
                                .setScale(2, RoundingMode.HALF_UP))
                        .orElse(null);
            }
            r.setSellingPrice(selling);
        });
        return list;
    }
}
