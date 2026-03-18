package com.g42.platform.gms.service_ticket_management.domain.enums;

/**
 * Enum representing the role of a staff member in a service ticket.
 * Used in service_ticket_assignment table to track who is assigned to each ticket.
 */
public enum RoleInTicket {
    RECEPTION,  // Lễ tân tiếp nhận
    ADVISOR,       // Tư vấn dịch vụ
    TECHNICIAN
}
