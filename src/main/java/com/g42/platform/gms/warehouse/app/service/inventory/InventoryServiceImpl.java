package com.g42.platform.gms.warehouse.app.service.inventory;

import com.g42.platform.gms.warehouse.api.dto.response.InventoryResponse;
import com.g42.platform.gms.warehouse.app.service.dto.StockRequest;
import com.g42.platform.gms.warehouse.app.service.dto.StockShortageInfo;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.domain.repository.StockEntryRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.CatalogItemJpaRepo;
import com.g42.platform.gms.warehouse.infrastructure.repository.WarehousePricingJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepo inventoryRepo;
    private final CatalogItemJpaRepo catalogItemJpaRepo;
    private final StockEntryRepo stockEntryRepo;
    private final WarehousePricingJpaRepo pricingRepo;

    @Override
    @Transactional(readOnly = true)
    public int getAvailableQuantity(Integer warehouseId, Integer itemId) {
        return inventoryRepo.findByWarehouseAndItem(warehouseId, itemId)
                .map(inv -> Math.max(0, inv.getQuantity() - inv.getReservedQuantity()))
                .orElse(0);
    }

    @Override
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

    @Override
    @Transactional(readOnly = true)
    public List<InventoryResponse> listByWarehouse(Integer warehouseId,
                                                    boolean showImportPrice,
                                                    boolean showSellingPrice) {
        List<InventoryJpa> invList = inventoryRepo.findByWarehouse(warehouseId);

        List<Integer> itemIds = invList.stream().map(InventoryJpa::getItemId).collect(Collectors.toList());
        // Chỉ lấy catalog item có type = PART
        Map<Integer, CatalogItemJpa> catalogMap = catalogItemJpaRepo.findAllById(itemIds)
                .stream()
                .filter(c -> c.getItemType() == com.g42.platform.gms.warehouse.domain.enums.CatalogItemType.PART)
                .collect(Collectors.toMap(CatalogItemJpa::getItemId, c -> c));

        return invList.stream()
                .filter(inv -> catalogMap.containsKey(inv.getItemId())) // chỉ giữ PART
                .map(inv -> {
            InventoryResponse r = new InventoryResponse();
            r.setInventoryId(inv.getInventoryId());
            r.setWarehouseId(inv.getWarehouseId());
            r.setItemId(inv.getItemId());
            r.setQuantity(inv.getQuantity());
            r.setReservedQuantity(inv.getReservedQuantity());
            r.setAvailableQuantity(Math.max(0, inv.getQuantity() - inv.getReservedQuantity()));

            CatalogItemJpa catalog = catalogMap.get(inv.getItemId());
            if (catalog != null) {
                r.setItemName(catalog.getItemName());
                if (showSellingPrice) {
                    // Tầng 1: warehouse_pricing (giá thị trường)
                    // Tầng 2: import_price × markup_multiplier của lô FIFO đầu tiên
                    BigDecimal selling = pricingRepo
                            .findByWarehouseIdAndItemIdAndIsActiveTrue(warehouseId, inv.getItemId())
                            .map(WarehousePricingJpa::getSellingPrice)
                            .orElse(null);
                    if (selling == null) {
                        selling = stockEntryRepo.findFifoLots(warehouseId, inv.getItemId()).stream()
                                .findFirst()
                                .map(lot -> lot.getImportPrice()
                                        .multiply(lot.getMarkupMultiplier())
                                        .setScale(2, java.math.RoundingMode.HALF_UP))
                                .orElse(null);
                    }
                    r.setSellingPrice(selling);
                }
                if (showImportPrice) {
                    stockEntryRepo.findFifoLots(warehouseId, inv.getItemId()).stream()
                            .findFirst()
                            .ifPresent(lot -> r.setImportPrice(lot.getImportPrice()));
                }
            }

            return r;
        }).collect(Collectors.toList());
    }
}
