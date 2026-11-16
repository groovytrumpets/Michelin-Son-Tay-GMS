package com.g42.platform.gms.service_ticket_management.api.dto.manage;

import lombok.Data;

/**
 * Body của POST /api/service-ticket/manage/{serviceTicketId}/reassign-customer
 * (và tham số của bản xem trước GET .../reassign-customer/preview).
 *
 * <p>Xe đích: chọn một xe CÓ SẴN của khách đích ({@link #targetVehicleId}), hoặc nhập biển số
 * ({@link #licensePlate}) — khách đích đã có xe biển đó thì dùng lại, chưa có thì tạo mới.
 * Bỏ trống cả hai chỉ được khi phiếu vốn không gắn xe (VD phiếu bán phụ tùng).
 */
@Data
public class ReassignCustomerRequest {
    /** Khách hàng đúng mà phiếu phải thuộc về. Bắt buộc. */
    private Integer targetCustomerId;
    /** Xe có sẵn của khách đích. */
    private Integer targetVehicleId;
    /** Biển số khi xe chưa có trong hồ sơ khách đích. */
    private String licensePlate;
    /** Loại & kiểu xe cho xe tạo mới (bỏ trống = lấy theo xe cũ nếu cùng biển số). */
    private String vehicleModel;
    /**
     * Bỏ xe đang gắn phiếu khỏi hồ sơ khách cũ. Chỉ được khi xe đó không còn lịch sử nào
     * khác ngoài phiếu này (xem preview.oldVehicle.canRemove).
     */
    private Boolean removeOldVehicle;
    /** Lý do. Bắt buộc khi chuyển thật, tối đa 500 ký tự. */
    private String reason;
}
