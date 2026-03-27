package com.g42.platform.gms.service_ticket_management.domain.repository;

import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspectionTire;

import java.util.List;

/**
 * Domain repository interface for SafetyInspectionTire.
 */
public interface SafetyInspectionTireRepo {

    List<SafetyInspectionTire> findByInspectionId(Integer inspectionId);

    SafetyInspectionTire save(SafetyInspectionTire tire);

    void deleteAll(List<SafetyInspectionTire> tires);

    void delete(SafetyInspectionTire tire);
}
