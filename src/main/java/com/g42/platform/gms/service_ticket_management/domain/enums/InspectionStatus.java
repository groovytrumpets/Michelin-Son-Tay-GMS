package com.g42.platform.gms.service_ticket_management.domain.enums;

/**
 * Enum representing the status of a safety inspection.
 * 
 * PENDING: Inspection enabled but not yet completed
 * COMPLETED: Inspection data saved
 * SKIPPED: Inspection disabled/skipped
 */
public enum InspectionStatus {
    PENDING,
    COMPLETED,
    SKIPPED
}
