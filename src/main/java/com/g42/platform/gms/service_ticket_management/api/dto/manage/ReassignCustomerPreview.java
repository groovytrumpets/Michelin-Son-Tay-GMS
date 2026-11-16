package com.g42.platform.gms.service_ticket_management.api.dto.manage;

import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Xem trước (applied = false) hoặc kết quả (applied = true) của việc chuyển phiếu sang khách khác.
 * Gọi xem trước khi chưa chọn khách đích thì {@link #to} = null — popup vẫn dùng được để biết
 * phiếu có bị khoá vì đã thu tiền không và xem lịch sử các lần chuyển trước.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReassignCustomerPreview {

    private boolean applied;

    private Integer serviceTicketId;
    private String ticketCode;
    private TicketStatus ticketStatus;

    /** Phiếu đã thu tiền (PAID hoặc có hoá đơn đã thu / thu một phần). */
    private boolean paid;
    /** Người đang thao tác được phép chuyển phiếu này không (phiếu đã thu tiền: chỉ ADMIN / MANAGER). */
    private boolean allowed;
    /** Lý do không được chuyển (null khi allowed). */
    private String blockedReason;

    /** Khách + xe hiện tại của phiếu. */
    private Party from;
    /** Khách + xe sau khi chuyển (null khi chưa chọn khách đích). */
    private Party to;
    /**
     * Xe đích là gì: EXISTING (xe có sẵn của khách đích), NEW (tạo xe mới cho khách đích),
     * TRANSFER (chuyển hẳn dòng xe cũ sang khách đích, đổi biển số nếu cần), NONE (phiếu không gắn xe).
     */
    private String vehicleMode;

    /** Xe đang gắn phiếu — để popup hỏi có bỏ khỏi hồ sơ khách cũ không. Null khi phiếu không gắn xe. */
    private OldVehicle oldVehicle;

    /** Dữ liệu đi kèm phiếu sẽ / đã được chuyển theo (lịch hẹn, số km, nhắc lịch...). */
    private List<String> moved;
    private List<String> warnings;

    /** Các lần chuyển trước của phiếu này, mới nhất trước. */
    private List<HistoryLine> history;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Party {
        private Integer customerId;
        private String customerCode;
        private String name;
        private String phone;
        private boolean walkIn;
        private Integer vehicleId;
        private String licensePlate;
        private String vehicleModel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OldVehicle {
        private Integer vehicleId;
        private String licensePlate;
        /** Xe này có thuộc khách cũ của phiếu không (thường là có). */
        private boolean ownedByFromCustomer;
        /** Số dòng lịch sử KHÁC (ngoài phiếu này) đang dùng xe — 0 thì bỏ khỏi hồ sơ khách cũ được. */
        private long otherHistoryCount;
        /** Mô tả lịch sử khác, VD "2 phiếu dịch vụ, 1 lượt sổ cũ". */
        private String otherHistory;
        private boolean canRemove;
        /** Kết quả (chỉ có khi applied): KEPT / TRANSFERRED / DELETED. */
        private String action;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HistoryLine {
        private Integer changeId;
        private String fromLabel;
        private String toLabel;
        private String oldVehicleAction;
        private boolean wasPaid;
        private String reason;
        private String changedByName;
        private LocalDateTime changedAt;
    }
}
