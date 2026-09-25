package com.g42.platform.gms.service_ticket_management.domain.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.BackfillKind;
import com.g42.platform.gms.service_ticket_management.domain.enums.BackfillReviewStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.EntryMode;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain entity representing a Service Ticket.
 * Simple POJO following the booking pattern - business logic in Service layer.
 */
@Data
public class ServiceTicket {
    
    private Integer serviceTicketId;
    private String ticketCode;
    private Integer bookingId;
    private Integer vehicleId;
    private Integer customerId;
    private Integer createdBy;
    private TicketStatus ticketStatus;
    private TicketType ticketType;
    private LocalDateTime receivedAt;
    private String customerRequest;
    private String technicianNotes;
    private LocalDateTime deliveredAt;
    private LocalDateTime estimatedDeliveryAt;
    private Boolean isDeleted;
    private String checkInNotes;
    private Boolean immutable;
    private Boolean safetyInspectionEnabled;
    private Boolean isPrinted;
    private LocalDateTime printedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private Integer queueNumber;

    // Nhập bù phiếu ngày trước (xem TicketBackfillService). Phiếu thường: entryMode = NORMAL, còn lại null.
    private EntryMode entryMode;
    private BackfillKind backfillKind;
    private Integer backfillParentTicketId;
    private String backfillReason;
    private String backfillPaymentMethod;
    private Integer backfillAdvisorId;
    private Integer backfillTechnicianId;
    private BackfillReviewStatus backfillReviewStatus;
    private Integer backfillReviewedBy;
    private LocalDateTime backfillReviewedAt;
    private String backfillReviewNote;

    // Bán cho khách lẻ vãng lai (xem PartsSaleService). Phiếu thường: isWalkIn = false và các
    // cột walkIn* để trống; phiếu khách lẻ trỏ customerId về hồ sơ dùng chung "Khách lẻ" nên
    // tên người mua thật chỉ có ở đây.
    private Boolean isWalkIn;
    private String walkInName;
    private String walkInPhone;
    private String walkInAddress;

    // Xưởng làm phiếu (Liquibase 044). Để null lúc tạo thì BranchStampListener tự điền.
    private Integer branchId;

    // List of photo IDs (not full objects - MapStruct will handle conversion)
    private List<Integer> photoIds = new ArrayList<>();
    
    /**
     * Initialize default values.
     * Sets status to DRAFT for new tickets (check-in in progress).
     */
    public void initializeDefaults() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (ticketStatus == null) {
            ticketStatus = TicketStatus.CREATED;
        }
        if (ticketType == null) {
            ticketType = TicketType.SERVICE;
        }
        if (immutable == null) {
            immutable = false;
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
        if (isWalkIn == null) {
            isWalkIn = false;
        }
    }
}
