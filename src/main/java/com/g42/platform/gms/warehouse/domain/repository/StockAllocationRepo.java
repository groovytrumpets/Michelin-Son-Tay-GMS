package com.g42.platform.gms.warehouse.domain.repository;

import com.g42.platform.gms.warehouse.domain.enums.AllocationStatus;
import com.g42.platform.gms.warehouse.domain.entity.StockAllocation;

import java.util.List;
import java.util.Optional;

/**
 * Repository Port — kế hoạch lưu trữ StockAllocation (giữ chỗ hàng)
 * 
 * Dữ liệu được lưu:
 * - service_ticket_id: Mã yêu cầu sửa chữa
 * - estimate_item_id: Dòng item từ báo giá
 * - warehouse_id: Kho nào
 * - item_id: Mặt hàng nào
 * - quantity: Số lượng giữ chỗ
 * - status: RESERVED (chưa xuất) / COMMITTED (đã xuất) / RELEASED (hủy)
 * - issue_id: Phiếu xuất kho gắn với allocation (nếu có)
 */
public interface StockAllocationRepo {

    /**
     * Lấy 1 allocation theo ID
     * @param allocationId ID của allocation
     * @return Optional chứa allocation hoặc empty nếu không tìm thấy
     */
    Optional<StockAllocation> findById(Integer allocationId);

    /**
     * Lấy tất cả allocations của 1 service ticket với status cụ thể
     * Dùng để: tìm tất cả allocations RESERVED của 1 yêu cầu sửa chữa
     * 
     * @param serviceTicketId ID của yêu cầu sửa chữa
     * @param status RESERVED / COMMITTED / RELEASED
     * @return Danh sách allocations (có thể từ nhiều warehouse khác nhau)
     */
    List<StockAllocation> findByTicketAndStatus(Integer serviceTicketId, AllocationStatus status);

    /**
     * Lấy allocations của 1 service ticket + 1 kho + 1 status
     * Dùng để: tạo phiếu xuất từ 1 kho cụ thể (nếu service ticket lấy hàng từ nhiều kho)
     * 
     * @param serviceTicketId ID yêu cầu sửa chữa
     * @param warehouseId ID kho
     * @param status RESERVED / COMMITTED / RELEASED
     * @return Danh sách allocations của ticket trong kho cụ thể
     */
    List<StockAllocation> findByTicketAndWarehouseAndStatus(Integer serviceTicketId, Integer warehouseId, AllocationStatus status);

    /**
     * Lấy tất cả allocations của 1 phiếu xuất kho với status cụ thể
     * Dùng để: hủy toàn bộ allocations của 1 phiếu, hoặc commit allocations khi confirm
     * 
     * @param issueId ID phiếu xuất kho
     * @param status RESERVED / COMMITTED / RELEASED
     * @return Danh sách allocations gắn với phiếu này
     */
    List<StockAllocation> findByIssueIdAndStatus(Integer issueId, AllocationStatus status);

    /**
     * Lấy tất cả allocations từ 1 dòng item báo giá
     * Dùng để: hủy allocation của estimate item cụ thể (chưa tạo phiếu xuất)
     * 
     * @param estimateItemId ID dòng item trong báo giá
     * @return Danh sách allocations (thường chỉ có 1)
     */
    List<StockAllocation> findByEstimateItemId(Integer estimateItemId);

    /**
     * Lưu allocation vào database
     * Dùng để: tạo allocation mới hoặc cập nhật allocation cũ
     * 
     * @param allocation Entity cần lưu (có thể là new hoặc existing)
     * @return Allocation sau khi lưu (có ID tự động sinh)
     */
    StockAllocation save(StockAllocation allocation);
}
