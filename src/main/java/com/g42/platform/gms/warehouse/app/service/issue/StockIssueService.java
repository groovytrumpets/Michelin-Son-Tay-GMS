package com.g42.platform.gms.warehouse.app.service.issue;

import com.g42.platform.gms.warehouse.api.dto.issue.CreateStockIssueRequest;
import com.g42.platform.gms.warehouse.api.dto.request.PatchIssueItemRequest;
import com.g42.platform.gms.warehouse.api.dto.request.UpdateStockIssueRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueDetailResponse;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueResponse;

import java.util.List;

public interface StockIssueService {

    /** Tạo phiếu xuất kho (DRAFT), kiểm tra available_quantity */
    StockIssueResponse create(CreateStockIssueRequest request, Integer staffId);

    /** Cập nhật từng item trong phiếu xuất — chỉ khi DRAFT */
    StockIssueResponse patchItem(Integer issueId, Integer issueItemId, PatchIssueItemRequest request);

    /** Cập nhật phiếu xuất kho — chỉ khi DRAFT */
    StockIssueResponse update(Integer issueId, UpdateStockIssueRequest request);

    /** Xác nhận phiếu xuất → trừ current_quantity, snapshot import_price, sinh Receipt */
    StockIssueResponse confirm(Integer issueId, Integer staffId);

    /** Lấy chi tiết phiếu xuất kèm receipt */
    StockIssueDetailResponse getDetail(Integer issueId);

    /** Danh sách phiếu xuất theo kho, mới nhất trước */
    List<StockIssueResponse> listByWarehouse(Integer warehouseId);
}
