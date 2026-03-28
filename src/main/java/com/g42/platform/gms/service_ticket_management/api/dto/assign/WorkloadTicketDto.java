package com.g42.platform.gms.service_ticket_management.api.dto.assign;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho ticket trong workload của staff
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkloadTicketDto {
    private String ticketCode;
    private String ticketStatus;
    private String roleInTicket;
    private String assignmentStatus;
    private String customerName;
    private String vehicleInfo;
}