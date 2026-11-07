package com.g42.platform.gms.service_ticket_management.api.dto.manage;

import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Xem trước (applied = false) hoặc kết quả (applied = true) của việc huỷ phiếu dịch vụ trùng.
 * Cùng một hình dạng để popup xác nhận và thông báo kết quả đọc chung một kiểu dữ liệu.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateCancelSummary {

    private boolean applied;

    /** Phiếu sẽ bị huỷ. */
    private TicketBrief ticket;
    /** Phiếu gốc được giữ lại (null khi xem trước mà chưa nhập mã phiếu gốc). */
    private TicketBrief original;

    /** Hoá đơn sẽ / đã chuyển sang VOID. */
    private List<BillLine> bills;
    /** Tổng tiền các hoá đơn ĐÃ THU sẽ bị loại khỏi doanh thu. */
    private BigDecimal paidAmountVoided;

    /** Hàng sẽ / đã được nhả giữ hoặc trả về kho. */
    private List<StockLine> stockLines;
    private int draftIssuesCancelled;
    private int confirmedIssuesReversed;
    private int openReturnsCancelled;

    /** Lưu ý cho người huỷ (VD: thông báo đánh giá đã gửi cho khách thì không thu hồi được). */
    private List<String> warnings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketBrief {
        private Integer serviceTicketId;
        private String ticketCode;
        private TicketStatus ticketStatus;
        private TicketType ticketType;
        private LocalDateTime receivedAt;
        private String customerName;
        private String customerPhone;
        private String licensePlate;
        private Integer customerId;
        private Integer vehicleId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BillLine {
        private Integer billId;
        private BigDecimal finalAmount;
        /** Trạng thái trước khi huỷ: UNPAID / PAID / ... */
        private String paymentStatus;
        private Instant paidAt;
        private int transactionCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockLine {
        /** RELEASE_RESERVED = nhả giữ hàng; RETURN_TO_STOCK = cộng lại tồn kho đã xuất. */
        private String action;
        private Integer warehouseId;
        private Integer itemId;
        private String itemName;
        private String sku;
        private BigDecimal quantity;
        private String issueCode;
    }
}
