package com.g42.platform.gms.warehouse.app.service.catalog;

import com.g42.platform.gms.warehouse.api.dto.request.CreatePartRequest;
import com.g42.platform.gms.warehouse.api.dto.response.PartResponse;

import java.util.List;

public interface WarehouseCatalogService {

    /** Tìm kiếm part/service theo tên, SKU, part number, barcode */
    List<PartResponse> search(String keyword);

    /** Tạo mới một PART (phụ tùng) — dùng khi nhập kho mà chưa có trong catalog */
    PartResponse createPart(CreatePartRequest request, Integer staffId);
}
