package com.g42.platform.gms.service_ticket_management.domain.repository;

import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspection;

import java.util.Optional;

/**
 * Domain repository interface for SafetyInspection.
 */
public interface SafetyInspectionRepo {

    Optional<SafetyInspection> findByServiceTicketId(Integer serviceTicketId);

    Optional<SafetyInspection> findById(Integer inspectionId);

    SafetyInspection save(SafetyInspection inspection);
}
