package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.WarehousePricing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Repository Port - lưu cấu hình giá bán theo kho và item.
 *
 * Trong luồng Issue:
 * - Nếu có giá active của kho -> dùng giá này làm export price
 * - Nếu không có -> fallback từ import_price * markup của lô FIFO
 */
public interface WarehousePricingRepo {

    /** Lấy tất cả giá active của 1 kho. */
    List<WarehousePricing> findActiveByWarehouse(Integer warehouseId);

    /** Search cấu hình giá có phân trang. */
    Page<WarehousePricing> search(Integer warehouseId,
                                  Boolean isActive,
                                  String search,
                                  Pageable pageable);

    /** Lấy giá active của 1 item trong 1 kho. */
    Optional<WarehousePricing> findActiveByWarehouseAndItem(Integer warehouseId, Integer itemId);

    /** Lấy pricing theo item + warehouse (không filter active) — dùng cho PricingService */
    Optional<WarehousePricing> findByItemIdAndWarehouseId(Integer itemId, Integer warehouseId);

    Optional<WarehousePricing> findById(Integer pricingId);

    WarehousePricing save(WarehousePricing pricing);
}
