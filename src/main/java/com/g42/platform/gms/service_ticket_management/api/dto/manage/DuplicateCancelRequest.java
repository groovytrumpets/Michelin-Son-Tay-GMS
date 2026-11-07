package com.g42.platform.gms.service_ticket_management.api.dto.manage;

import lombok.Data;

/** Body của POST /api/service-ticket/manage/{serviceTicketId}/cancel-duplicate. */
@Data
public class DuplicateCancelRequest {
    /** Mã phiếu GỐC (phiếu được giữ lại) mà phiếu này bị tạo trùng. Bắt buộc. */
    private String duplicateOfTicketCode;
    /** Lý do huỷ. Bắt buộc, tối đa 500 ký tự. */
    private String reason;
}
