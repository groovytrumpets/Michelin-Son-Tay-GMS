package com.g42.platform.gms.service_ticket_management.domain.enums;

/**
 * Enum representing the status of a service ticket in the check-in lifecycle.
 * 
 * CREATED: Initial state when check-in is completed (MVP focus)
 * DRAFT: Check-in in progress (not yet completed)
 * IN_PROGRESS: Service work has started
 * COMPLETED: Service work is finished
 * CANCELLED: Service ticket was cancelled
 */
public enum TicketStatus {
    CREATED,
    DRAFT,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
