package com.g42.platform.gms.warehouse.app.service.inventory;

import com.g42.platform.gms.warehouse.api.dto.response.InventoryResponse;
import com.g42.platform.gms.warehouse.app.service.dto.StockRequest;
import com.g42.platform.gms.warehouse.app.service.dto.StockShortageInfo;

import java.util.List;

public interface InventoryService {

    /** Lấy số lượng khả dụng = current_quantity - reserved_quantity */
    int getAvailableQuantity(Integer warehouseId, Integer itemId);

    /** Kiểm tra đủ hàng, trả về danh sách item thiếu */
    List<StockShortageInfo> checkAvailability(List<StockRequest> requests);

    /**
     * Lấy danh sách tồn kho theo kho, lọc field nhạy cảm theo role.
     * @param showImportPrice true nếu caller có quyền xem giá gốc (ACCOUNTANT/MANAGER/ADMIN)
     * @param showSellingPrice true nếu caller có quyền xem giá bán (ADVISOR+)
     */
    List<InventoryResponse> listByWarehouse(Integer warehouseId,
                                            boolean showImportPrice,
                                            boolean showSellingPrice);

    /**
     * Tìm kiếm tồn kho theo keyword (tên/SKU) — dùng cho màn hình nhập kho.
     * Trả về cả item chưa có trong inventory (quantity=0) để có thể nhập mới.
     */
    List<InventoryResponse> searchByWarehouse(Integer warehouseId, String keyword,
                                              boolean showImportPrice);

    /**
     * Lấy toàn bộ PART trong catalog kèm tồn kho hiện tại.
     * Item chưa có trong kho sẽ có quantity=0.
     */
    List<InventoryResponse> listAllPartsWithInventory(Integer warehouseId, boolean showImportPrice);

    void updateInventoryByEstimate(Integer itemId, Integer warehouseId,Integer quantity);

    void increaseReservedQuantity(Integer itemId, Integer warehouseId, Integer quantity);

    void updateReservedQuantityByDelta(Integer itemId, Integer warehouseId, int difference);

    void decreaseReservedQuantity(Integer itemId, Integer warehouseId, Integer quantity);
}
