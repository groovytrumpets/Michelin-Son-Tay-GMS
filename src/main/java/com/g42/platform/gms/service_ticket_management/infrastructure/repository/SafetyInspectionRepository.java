package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyInspectionJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SafetyInspectionRepository extends JpaRepository<SafetyInspectionJpa, Integer> {
    
    Optional<SafetyInspectionJpa> findByServiceTicketId(Integer serviceTicketId);
    
    boolean existsByServiceTicketId(Integer serviceTicketId);
}
