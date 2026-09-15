package com.g42.platform.gms.service_ticket_management.domain.enums;

/**
 * Loại nhập bù.
 * MISSED:     phiếu bị miss hoàn toàn, nhập lại cả phiếu (nhập lại).
 * SUPPLEMENT: phiếu gốc đã thanh toán nhưng quên dòng hàng/dịch vụ, bổ sung
 *             bằng một phiếu con trỏ về phiếu gốc (nhập thiếu).
 */
public enum BackfillKind {
    MISSED,
    SUPPLEMENT
}
