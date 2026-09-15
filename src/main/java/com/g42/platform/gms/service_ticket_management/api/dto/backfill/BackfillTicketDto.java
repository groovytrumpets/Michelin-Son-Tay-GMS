package com.g42.platform.gms.service_ticket_management.api.dto.backfill;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Một phiếu nhập bù ở màn duyệt. */
@Getter
@Setter
@NoArgsConstructor
public class BackfillTicketDto {
    private Integer serviceTicketId;
    private String ticketCode;
    private String ticketType;
    private String ticketStatus;
    private String kind;
    private String reviewStatus;

    private Integer parentTicketId;
    private String parentTicketCode;

    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private Integer vehicleId;
    private String licensePlate;

    /** Ngày giờ thực tế (received_at). */
    private LocalDateTime actualServiceAt;
    /** Lúc bấm nhập bù (created_at). */
    private LocalDateTime createdAt;
    /** Số ngày bị lùi = ngày nhập - ngày thực tế. */
    private Long daysLate;

    private Integer createdBy;
    private String createdByName;

    private String reason;
    private String note;
    private String paymentMethod;
    private Integer advisorId;
    private String advisorName;
    private Integer technicianId;
    private String technicianName;

    private Integer estimateId;
    private BigDecimal totalAmount;
    private long proofCount;

    private Integer reviewedBy;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private String reviewNote;

    /** Phiếu khác của cùng khách trong cùng ngày thực tế — dấu hiệu nhập trùng. */
    private List<BackfillDuplicateDto> sameDayTickets;
}
