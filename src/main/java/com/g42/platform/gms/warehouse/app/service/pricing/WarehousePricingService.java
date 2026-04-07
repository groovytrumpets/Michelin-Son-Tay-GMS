package com.g42.platform.gms.warehouse.app.service.pricing;

import com.g42.platform.gms.warehouse.api.dto.request.UpsertPricingRequest;
import com.g42.platform.gms.warehouse.api.dto.response.PricingResponse;

import java.util.List;

public interface WarehousePricingService {

    /** Danh sách giá theo kho */
    List<PricingResponse> listByWarehouse(Integer warehouseId);

    /** Tạo hoặc cập nhật giá cho 1 item trong kho */
    PricingResponse upsert(UpsertPricingRequest request);

    /** Vô hiệu hóa giá */
    void deactivate(Integer pricingId);
}
