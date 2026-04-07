package com.g42.platform.gms.service_ticket_management.api.dto.manage;

import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Response DTO for service ticket list view (receptionist).
 * Tương tự BookedRespond trong booking_management.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceQueueResponse {
    private Integer serviceTicketId;
    private String ticketCode;
    private Integer bookingId;
    private TicketStatus ticketStatus;
    private LocalDateTime receivedAt;
    private Integer queueNumber;
}
