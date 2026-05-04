package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.StockIssueItem;

import java.util.List;
import java.util.Optional;

public interface StockIssueItemRepo {
    /**
     * Repository port cho các dòng `StockIssueItem`.
     *
     * Ghi chú:
     * - `saveAll` được dùng để persist nhiều dòng khi tạo/refresh DRAFT items.
     * - `deleteByIssueId` thường được gọi khi tạo lại draft (clear old items) hoặc
     *   khi hủy phiếu DRAFT.
     * - Service nên đảm bảo rằng các thay đổi item được thực hiện trong cùng
     *   transaction với cập nhật `StockIssue` để tránh trạng thái không nhất quán.
     */
    List<StockIssueItem> saveAll(List<StockIssueItem> items);
    void deleteByIssueId(Integer issueId);
    void deleteById(Integer issueItemId);
    Optional<StockIssueItem> findById(Integer issueItemId);
    StockIssueItem save(StockIssueItem item);
    List<StockIssueItem> findByIssueId(Integer issueId);
}
