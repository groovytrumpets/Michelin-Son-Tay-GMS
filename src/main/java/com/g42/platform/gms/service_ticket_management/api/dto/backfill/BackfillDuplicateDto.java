package com.g42.platform.gms.service_ticket_management.api.dto.backfill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Phiếu đã có của cùng khách trong ngày — để cảnh báo nhập trùng. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BackfillDuplicateDto {
    private Integer serviceTicketId;
    private String ticketCode;
    private String ticketType;
    private String ticketStatus;
    private String entryMode;
    private LocalDateTime receivedAt;
}
