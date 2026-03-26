package com.g42.platform.gms.service_ticket_management.domain.entity;

import com.g42.platform.gms.service_ticket_management.domain.enums.AssignmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceTicketAssignment {

    private Integer assignmentId;
    private Integer serviceTicketId;
    private Integer staffId;
    private String roleInTicket;
    private Instant assignedAt;
    private Boolean isPrimary;
    private AssignmentStatus status;
    private String note;
}
