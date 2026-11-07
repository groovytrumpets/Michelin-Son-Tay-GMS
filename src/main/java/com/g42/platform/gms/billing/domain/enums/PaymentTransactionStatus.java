package com.g42.platform.gms.billing.domain.enums;

/**
 * VOID = giao dịch bị huỷ vì phiếu bị tạo trùng (tiền thật chỉ thu một lần ở phiếu gốc),
 * khác REFUNDED là đã trả lại tiền cho khách. Cột DB là ENUM — xem Liquibase 052-3.
 */
public enum PaymentTransactionStatus {
    SUCCESS, FAILED, REFUNDED, VOID
}
