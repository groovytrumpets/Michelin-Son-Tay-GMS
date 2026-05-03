package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.Inventory;

import java.util.List;
import java.util.Optional;

/**
 * Repository Port — kế hoạch lưu trữ Inventory (tồn kho)
 * 
 * Dữ liệu được lưu:
 * - warehouse_id: Kho nào
 * - item_id: Mặt hàng nào
 * - quantity: Tổng số hàng có
 * - reserved_quantity: Số hàng đang bị "giữ chỗ" (có allocation RESERVED)
 * 
 * Công thức:
 * - available = quantity - reserved_quantity (hàng còn trống)
 */
public interface InventoryRepo {

    /**
     * Lấy inventory của 1 hàng tại 1 kho (chỉ đọc, không lock)
     * Dùng để: hiển thị tồn kho, check status, không cần cập nhật
     * 
     * @param warehouseId ID kho
     * @param itemId ID mặt hàng
     * @return Optional chứa Inventory
     */
    Optional<Inventory> findByWarehouseAndItem(Integer warehouseId, Integer itemId);

    /**
     * Lấy inventory với "lock" để cập nhật an toàn (SELECT FOR UPDATE)
     * Tại sao dùng lock? Vì nhiều request có thể chỉnh sửa cùng lúc:
     * - Thread A: reserve 5 cái
     * - Thread B: reserve 3 cái (cùng hàng)
     * Nếu không lock, có thể đếm sai tồn kho!
     * 
     * Lock đảm bảo: chỉ 1 transaction được modify cùng lúc
     * 
     * @param warehouseId ID kho
     * @param itemId ID mặt hàng
     * @return Optional chứa Inventory (được lock)
     */
    Optional<Inventory> findByWarehouseAndItemWithLock(Integer warehouseId, Integer itemId);

    /**
     * Lấy tất cả inventory của 1 kho
     * Dùng để: báo cáo tồn kho, list hàng có tại kho
     * 
     * @param warehouseId ID kho
     * @return Danh sách tất cả inventory trong kho
     */
    List<Inventory> findByWarehouse(Integer warehouseId);

    /**
     * Lấy những hàng sắp hết tồn (quantity < threshold)
     * Dùng để: cảnh báo cần nhập hàng, báo cáo tồn kho thấp
     * 
     * @param warehouseId ID kho
     * @return Danh sách hàng có tồn kho dưới ngưỡng
     */
    List<Inventory> findLowStock(Integer warehouseId);

    /**
     * Cập nhật inventory vào database
     * Dùng để: sau khi thay đổi quantity hoặc reserved_quantity
     * 
     * Lưu ý: luôn dùng findByWarehouseAndItemWithLock() trước
     * để đảm bảo không bị race condition
     * 
     * @param inventory Entity Inventory sau khi cập nhật
     * @return Inventory sau khi lưu
     */
    Inventory save(Inventory inventory);
}
