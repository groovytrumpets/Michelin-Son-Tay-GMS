package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.entity.StockIssue;
import com.g42.platform.gms.warehouse.domain.enums.IssueType;
import com.g42.platform.gms.warehouse.domain.enums.StockIssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StockIssueRepo {
    /**
     * Repository port cho `StockIssue`.
     *
     * Mô tả:
     * - Chứa các truy vấn để tìm phiếu theo warehouse / service ticket, kiểm tra
     *   tồn tại phiếu DRAFT/CONFIRMED và tìm draft hiện tại để merge items.
     * - `StockIssueService` sẽ sử dụng các method ở đây để: tạo DRAFT, cập nhật
     *   trạng thái (DRAFT → CONFIRMED), và kiểm tra xem đã có phiếu CONFIRMED
     *   cho một service ticket hay chưa (để tránh tạo 2 phiếu xác nhận).
     *
     * Ghi chú:
     * - Các phương thức `exists*` giúp service ngăn chặn duplicate confirms.
     * - `findDraftServiceTicketIssueInWarehouse` thường được dùng khi muốn
     *   merge allocations vào một draft hiện có của cùng warehouse.
     */
    Optional<StockIssue> findById(Integer issueId);
    List<StockIssue> findByWarehouseId(Integer warehouseId);
    List<StockIssue> findByServiceTicketId(Integer serviceTicketId);
    Page<StockIssue> search(Integer warehouseId,
                            StockIssueStatus status,
                            IssueType issueType,
                            LocalDate fromDate,
                            LocalDate toDate,
                            String search,
                            Pageable pageable);
    StockIssue save(StockIssue issue);
    boolean existsByCode(String issueCode);
    boolean existsConfirmedServiceTicketIssue(Integer serviceTicketId);
    boolean existsDraftServiceTicketIssue(Integer serviceTicketId);
    boolean existsDraftServiceTicketIssueInWarehouse(Integer serviceTicketId, Integer warehouseId);

    Optional<StockIssue> findDraftServiceTicketIssueInWarehouse(Integer serviceTicketId, Integer warehouseId);
}