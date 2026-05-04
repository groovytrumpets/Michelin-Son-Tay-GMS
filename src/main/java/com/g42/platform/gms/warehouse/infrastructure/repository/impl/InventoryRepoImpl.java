package com.g42.platform.gms.warehouse.infrastructure.repository.impl;

import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.repository.InventoryRepo;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import com.g42.platform.gms.warehouse.infrastructure.repository.InventoryJpaRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Infrastructure Adapter: implements InventoryRepo (domain port).
 *
 * ┌──────────────────────────────────────────────────────────────────┐
 * │  Luồng dữ liệu khi confirm phiếu nhập (StockEntryService):       │
 * │                                                                   │
 * │  1. inventoryRepo.findByWarehouseAndItemWithLock(wId, iId)        │
 * │       → InventoryRepoImpl.findByWarehouseAndItemWithLock(...)     │
 * │       → jpaRepo.findByWarehouseIdAndItemIdWithLock(...)           │
 * │       → SQL: SELECT * FROM inventory                              │
 * │               WHERE warehouse_id = ? AND item_id = ?             │
 * │               FOR UPDATE  ← khóa row                             │
 * │       → map InventoryJpa → Inventory domain                       │
 * │       → trả về Optional<Inventory>                                │
 * │                                                                   │
 * │  2. Service tính: balanceAfter = inventory.quantity + item.qty    │
 * │     inventory.setQuantity(balanceAfter)                           │
 * │                                                                   │
 * │  3. inventoryRepo.save(inventory)                                 │
 * │       → InventoryRepoImpl.save(...)                               │
 * │       → toJpa(inventory) → InventoryJpa                           │
 * │       → jpaRepo.save(jpa)                                         │
 * │       → SQL: UPDATE inventory SET quantity = ? WHERE inventory_id = ?
 * │       → map InventoryJpa → Inventory domain                       │
 * │       → trả về Inventory đã cập nhật                              │
 * │                                                                   │
 * │  4. @Transactional commit → release SELECT FOR UPDATE lock        │
 * └──────────────────────────────────────────────────────────────────┘
 *
 * Về reservedQuantity:
 *   - quantity = tổng tồn kho thực tế (đã nhập, chưa xuất)
 *   - reservedQuantity = số lượng đã được "đặt chỗ" cho phiếu xuất đang xử lý
 *   - availableQuantity = quantity - reservedQuantity (có thể xuất ngay)
 *   - Khi confirm nhập: quantity tăng, reservedQuantity không đổi
 *   - Khi tạo phiếu xuất: reservedQuantity tăng (giữ chỗ)
 *   - Khi confirm xuất: quantity giảm, reservedQuantity giảm
 */
@Repository
@RequiredArgsConstructor
public class InventoryRepoImpl implements InventoryRepo {

    private final InventoryJpaRepo jpaRepo;

    /**
     * Đọc tồn kho mà KHÔNG khóa row.
     * SQL: SELECT * FROM inventory WHERE warehouse_id = ? AND item_id = ?
     * Dùng khi: chỉ cần đọc để hiển thị, không có ý định ghi.
     */
    @Override
    public Optional<Inventory> findByWarehouseAndItem(Integer warehouseId, Integer itemId) {
        return jpaRepo.findByWarehouseIdAndItemId(warehouseId, itemId).map(this::toDomain);
    }

    /**
     * Đọc tồn kho VÀ khóa row (SELECT FOR UPDATE).
     *
     * SQL: SELECT * FROM inventory
     *      WHERE warehouse_id = ? AND item_id = ?
     *      FOR UPDATE
     *
     * Dùng khi: service sẽ tính lại quantity/reservedQuantity và save ngay sau đó.
     * Lock tự động release khi @Transactional commit hoặc rollback.
     *
     * Nếu row chưa tồn tại (sản phẩm mới nhập lần đầu vào kho):
     *   → trả về Optional.empty()
     *   → Service tạo mới Inventory với quantity = 0 rồi cộng thêm
     */
    @Override
    public Optional<Inventory> findByWarehouseAndItemWithLock(Integer warehouseId, Integer itemId) {
        return jpaRepo.findByWarehouseIdAndItemIdWithLock(warehouseId, itemId).map(this::toDomain);
    }

    /**
     * Lấy toàn bộ tồn kho của 1 kho.
     * SQL: SELECT * FROM inventory WHERE warehouse_id = ?
     * Dùng cho: màn hình tổng quan tồn kho, kiểm kê.
     */
    @Override
    public List<Inventory> findByWarehouse(Integer warehouseId) {
        return jpaRepo.findByWarehouseId(warehouseId).stream().map(this::toDomain).toList();
    }

    /**
     * Lấy danh sách sản phẩm sắp hết hàng.
     * SQL (JPQL):
     *   SELECT * FROM inventory
     *   WHERE warehouse_id = ?
     *     AND (quantity - reserved_quantity) <= min_stock_level
     * Dùng cho: cảnh báo tồn kho thấp, dashboard.
     */
    @Override
    public List<Inventory> findLowStock(Integer warehouseId) {
        return jpaRepo.findLowStockByWarehouse(warehouseId).stream().map(this::toDomain).toList();
    }

    /**
     * Lưu tồn kho (INSERT hoặc UPDATE).
     *
     * Luồng:
     *   toJpa(inventory) → InventoryJpa
     *   jpaRepo.save(jpa):
     *     - inventoryId = null → INSERT INTO inventory (...)
     *     - inventoryId != null → UPDATE inventory SET quantity = ?, ... WHERE inventory_id = ?
     *   toDomain(saved) → trả về Inventory với inventoryId đã được DB sinh ra
     */
    @Override
    public Inventory save(Inventory inventory) {
        return toDomain(jpaRepo.save(toJpa(inventory)));
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    /** InventoryJpa → Inventory domain */
    private Inventory toDomain(InventoryJpa jpa) {
        return Inventory.builder()
                .inventoryId(jpa.getInventoryId())
                .warehouseId(jpa.getWarehouseId())
                .itemId(jpa.getItemId())
                .quantity(jpa.getQuantity())
                .reservedQuantity(jpa.getReservedQuantity())
                .minStockLevel(jpa.getMinStockLevel())
                .maxStockLevel(jpa.getMaxStockLevel())
                .lastUpdated(jpa.getLastUpdated())
                .build();
    }

    /** Inventory domain → InventoryJpa. Null-safe: các số lượng mặc định = 0. */
    private InventoryJpa toJpa(Inventory domain) {
        InventoryJpa jpa = new InventoryJpa();
        jpa.setInventoryId(domain.getInventoryId());
        jpa.setWarehouseId(domain.getWarehouseId());
        jpa.setItemId(domain.getItemId());
        jpa.setQuantity(domain.getQuantity() != null ? domain.getQuantity() : 0);
        jpa.setReservedQuantity(domain.getReservedQuantity() != null ? domain.getReservedQuantity() : 0);
        jpa.setMinStockLevel(domain.getMinStockLevel() != null ? domain.getMinStockLevel() : 0);
        jpa.setMaxStockLevel(domain.getMaxStockLevel() != null ? domain.getMaxStockLevel() : 0);
        return jpa;
    }
}
