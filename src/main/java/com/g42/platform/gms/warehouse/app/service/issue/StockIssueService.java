package com.g42.platform.gms.warehouse.app.service.issue;

import com.g42.platform.gms.warehouse.api.dto.issue.CreateStockIssueRequest;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueDetailResponse;
import com.g42.platform.gms.warehouse.api.dto.response.StockIssueResponse;

public interface StockIssueService {

    /** Tạo phiếu xuất kho (DRAFT), kiểm tra available_quantity */
    StockIssueResponse create(CreateStockIssueRequest request, Integer staffId);

    /** Xác nhận phiếu xuất → trừ current_quantity, snapshot import_price, sinh Receipt */
    StockIssueResponse confirm(Integer issueId, Integer staffId);

    /** Lấy chi tiết phiếu xuất kèm receipt */
    StockIssueDetailResponse getDetail(Integer issueId);
}
