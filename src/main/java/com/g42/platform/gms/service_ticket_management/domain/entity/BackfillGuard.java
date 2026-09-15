package com.g42.platform.gms.service_ticket_management.domain.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.BackfillReviewStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.EntryMode;

/**
 * Chặn các luồng cũ (chốt phiếu bán hàng, huỷ giữ hàng, đổi trạng thái, tạo hoá đơn,
 * thu tiền) chạm vào phiếu nhập bù chưa được quản lý duyệt.
 *
 * Phiếu nhập bù đang HOLDING nên hiện chung ở /parts-sale-ticket-management và
 * /service-ticket-management — không chặn thì lễ tân bấm "Thanh toán" ở đó là
 * đi vòng qua bước duyệt, đúng kiểu phiếu khống mà tính năng này muốn ngăn.
 * Chỉ TicketBackfillService.approve/reject được đổi trạng thái phiếu này.
 */
public final class BackfillGuard {

    private BackfillGuard() {
    }

    public static boolean isUnapprovedBackfill(EntryMode entryMode, BackfillReviewStatus reviewStatus) {
        return entryMode == EntryMode.BACKFILL && reviewStatus != BackfillReviewStatus.APPROVED;
    }

    public static void requireNotUnapprovedBackfill(EntryMode entryMode, BackfillReviewStatus reviewStatus,
                                                    String ticketCode) {
        if (!isUnapprovedBackfill(entryMode, reviewStatus)) {
            return;
        }
        String code = ticketCode == null ? "" : " " + ticketCode;
        if (reviewStatus == BackfillReviewStatus.REJECTED) {
            throw new IllegalArgumentException("Phiếu nhập bù" + code + " đã bị từ chối, không thao tác tiếp được.");
        }
        throw new IllegalArgumentException("Phiếu nhập bù" + code
                + " đang chờ quản lý duyệt — chỉ thao tác được ở màn Duyệt phiếu nhập bù.");
    }

    public static void requireNotUnapprovedBackfill(ServiceTicket ticket) {
        if (ticket != null) {
            requireNotUnapprovedBackfill(ticket.getEntryMode(), ticket.getBackfillReviewStatus(), ticket.getTicketCode());
        }
    }
}
