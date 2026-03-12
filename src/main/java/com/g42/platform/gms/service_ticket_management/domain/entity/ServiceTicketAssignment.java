package com.g42.platform.gms.service_ticket_management.domain.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.RoleInTicket;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Domain entity representing a Service Ticket Assignment.
 * Tracks which staff members are assigned to a service ticket and their roles.
 */
@Data
public class ServiceTicketAssignment {
    private Integer assignmentId;
    private Integer serviceTicketId;
    private Integer staffId;
    private RoleInTicket roleInTicket;
    private LocalDateTime assignedAt;
}
