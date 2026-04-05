package com.g42.platform.gms.service_ticket_management.infrastructure.entity;

import com.g42.platform.gms.auth.entity.StaffProfile;
import com.g42.platform.gms.service_ticket_management.domain.enums.AssignmentStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.RoleInTicket;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "service_ticket_assignment", schema = "michelin_garage")
public class ServiceTicketAssignmentJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id", nullable = false)
    private Integer assignmentId;

    @NotNull
    @Column(name = "service_ticket_id", nullable = false)
    private Integer serviceTicketId;

    @NotNull
    @Column(name = "staff_id", nullable = false)
    private Integer staffId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role_in_ticket", nullable = false, length = 50)
    private RoleInTicket roleInTicket;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "assigned_at")
    private Instant assignedAt;

    @ColumnDefault("0")
    @Column(name = "is_primary")
    private Boolean isPrimary;

    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'PENDING'")
    @Column(name = "status", nullable = false)
    private AssignmentStatus status;

    @Size(max = 255)
    @Column(name = "note")
    private String note;

    // Relationship với ServiceTicket để lấy thông tin chi tiết
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_ticket_id", insertable = false, updatable = false)
    private ServiceTicketJpa serviceTicket;
}
