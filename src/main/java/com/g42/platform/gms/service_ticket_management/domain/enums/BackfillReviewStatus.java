package com.g42.platform.gms.service_ticket_management.domain.enums;

/**
 * Trạng thái duyệt của phiếu nhập bù.
 * PENDING_REVIEW: đã giữ hàng, chờ quản lý duyệt.
 * APPROVED:       đã duyệt — đã xuất kho và ghi thanh toán theo ngày thực tế.
 * REJECTED:       bị từ chối — đã nhả hàng về kho và huỷ phiếu.
 */
public enum BackfillReviewStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED
}
