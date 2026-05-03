package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.StockIssueItem;

import java.util.List;
import java.util.Optional;

/**
 * Repository Port - lưu trữ dòng chi tiết của phiếu xuất (StockIssueItem).
 *
 * Mỗi record là 1 dòng item trong phiếu:
 * - issue_item_id
 * - issue_id
 * - item_id
 * - entry_item_id (lô nhập đã chọn theo FIFO)
 * - quantity, export_price, import_price, discount_rate, final_price
 */
public interface StockIssueItemRepo {

    /** Lưu batch nhiều dòng item cùng lúc (thường dùng khi create/update draft). */
    List<StockIssueItem> saveAll(List<StockIssueItem> items);

    /** Xóa toàn bộ item của 1 phiếu (thường dùng khi replace lại items lúc update). */
    void deleteByIssueId(Integer issueId);

    /** Xóa 1 dòng item cụ thể theo issueItemId. */
    void deleteById(Integer issueItemId);

    /** Lấy 1 dòng item theo ID. */
    Optional<StockIssueItem> findById(Integer issueItemId);

    /** Lưu 1 dòng item. */
    StockIssueItem save(StockIssueItem item);

    /** Lấy toàn bộ item của 1 issue. */
    List<StockIssueItem> findByIssueId(Integer issueId);
}
