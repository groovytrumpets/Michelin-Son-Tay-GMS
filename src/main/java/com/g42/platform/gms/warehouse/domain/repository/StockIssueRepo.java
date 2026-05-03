package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.StockIssue;
import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository Port - lưu trữ phiếu xuất kho (StockIssue).
 *
 * Dữ liệu chính của 1 phiếu xuất:
 * - issue_id: ID phiếu
 * - issue_code: Mã phiếu (ví dụ XK-20260503-1)
 * - warehouse_id: Xuất từ kho nào
 * - issue_type: SERVICE_TICKET / WHOLESALE / ...
 * - service_ticket_id: Gắn với ticket nào (nếu có)
 * - status: DRAFT / CONFIRMED / CANCELLED
 */
public interface StockIssueRepo {

    /** Lấy 1 phiếu theo ID */
    Optional<StockIssue> findById(Integer issueId);

    /** Lấy tất cả phiếu của 1 kho */
    List<StockIssue> findByWarehouseId(Integer warehouseId);

    /** Lấy tất cả phiếu gắn với 1 service ticket */
    List<StockIssue> findByServiceTicketId(Integer serviceTicketId);

    /**
     * Tìm kiếm có filter + phân trang.
     * Dùng cho màn list phiếu xuất kho trên UI.
     */
    Page<StockIssue> search(Integer warehouseId,
                            StockIssueStatus status,
                            IssueType issueType,
                            LocalDate fromDate,
                            LocalDate toDate,
                            String search,
                            Pageable pageable);

    /** Tạo mới hoặc cập nhật phiếu */
    StockIssue save(StockIssue issue);

    /** Kiểm tra trùng mã phiếu */
    boolean existsByCode(String issueCode);

    /** Kiểm tra ticket đã có phiếu CONFIRMED chưa */
    boolean existsConfirmedServiceTicketIssue(Integer serviceTicketId);

    /** Kiểm tra ticket đã có phiếu DRAFT chưa */
    boolean existsDraftServiceTicketIssue(Integer serviceTicketId);

    /** Kiểm tra ticket đã có phiếu DRAFT trong 1 kho cụ thể chưa */
    boolean existsDraftServiceTicketIssueInWarehouse(Integer serviceTicketId, Integer warehouseId);

    /**
     * Lấy phiếu DRAFT của 1 service ticket trong 1 kho cụ thể.
     * Dùng khi cần mở lại phiếu draft hiện có thay vì tạo mới.
     */
    Optional<StockIssue> findDraftServiceTicketIssueInWarehouse(Integer serviceTicketId, Integer warehouseId);
}
