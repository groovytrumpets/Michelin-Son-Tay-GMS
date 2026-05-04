package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import com.g42.platform.gms.warehouse.domain.entity.StockAllocation;

import java.util.List;
import java.util.Optional;

/**
 * Repository port cho `StockAllocation` — định nghĩa các truy vấn cần thiết
 * để Service có thể tạo/đọc/cập nhật allocations.
 *
 * Ghi chú sử dụng (services liên quan):
 * - `StockAllocationService` sử dụng để reserve, commit và tìm allocations theo
 *   estimateItemId / serviceTicket. Khi thay đổi reserved quantities, Service
 *   phải đảm bảo cập nhật `Inventory` tương ứng (thường thông qua
 *   `InventoryRepo.findByWarehouseAndItemWithLock(...)`).
 * - `StockIssueService` dùng repo này để gắn allocation vào issue (setIssueId)
 *   và để chuyển trạng thái RESERVED → COMMITTED khi xác nhận xuất.
 *
 * Invariants / concurrency:
 * - Không ghi đè allocation đã ở trạng thái COMMITTED.
 * - Việc giảm `inventory.reservedQuantity` cần được thực hiện trong cùng
 *   transaction và với row lock để tránh race condition.
 */
public interface StockAllocationRepo {

    Optional<StockAllocation> findById(Integer allocationId);

    List<StockAllocation> findByTicketAndStatus(Integer serviceTicketId, AllocationStatus status);

    List<StockAllocation> findByTicketAndWarehouseAndStatus(Integer serviceTicketId, Integer warehouseId, AllocationStatus status);

    List<StockAllocation> findByIssueIdAndStatus(Integer issueId, AllocationStatus status);

    List<StockAllocation> findByEstimateItemId(Integer estimateItemId);

    StockAllocation save(StockAllocation allocation);
}
