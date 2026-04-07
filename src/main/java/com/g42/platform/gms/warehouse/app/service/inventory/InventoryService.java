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
}
