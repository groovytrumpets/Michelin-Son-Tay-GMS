package com.g42.platform.gms.service_ticket_management.infrastructure.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.RoleInTicket;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * JPA entity for service_ticket_assignment table.
 * 
 * Maps to the service_ticket_assignment table in the database.
 * This entity tracks which staff members are assigned to service tickets
 * and their roles (RECEPTIONIST, ADVISOR, TECHNICIAN, INSPECTOR).
 */
@Entity
@Table(name = "service_ticket_assignment", indexes = {
    @Index(name = "idx_service_ticket_id", columnList = "service_ticket_id"),
    @Index(name = "idx_staff_id", columnList = "staff_id"),
    @Index(name = "idx_role", columnList = "role_in_ticket")
})
@Data
public class ServiceTicketAssignmentJpa {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Integer assignmentId;
    
    @Column(name = "service_ticket_id", nullable = false)
    private Integer serviceTicketId;
    
    @Column(name = "staff_id", nullable = false)
    private Integer staffId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_ticket", nullable = false)
    private RoleInTicket roleInTicket;
    
    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
    
    @PrePersist
    protected void onCreate() {
        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }
    }
}
