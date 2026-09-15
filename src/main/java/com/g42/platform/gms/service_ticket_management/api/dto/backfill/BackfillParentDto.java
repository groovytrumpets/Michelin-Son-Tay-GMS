package com.g42.platform.gms.service_ticket_management.api.dto.backfill;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Phiếu gốc tra theo mã khi nhập thiếu dòng. */
@Getter
@Setter
@NoArgsConstructor
public class BackfillParentDto {
    private Integer serviceTicketId;
    private String ticketCode;
    private String ticketType;
    private String ticketStatus;
    private Integer customerId;
    private String customerName;
    private String customerPhone;
    private Integer vehicleId;
    private String licensePlate;
    private LocalDateTime receivedAt;
    private LocalDateTime deliveredAt;
}
