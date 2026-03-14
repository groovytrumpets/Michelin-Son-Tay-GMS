package com.g42.platform.gms.service_ticket_management.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

public class ServiceTicketAssignment {

    private Integer id;
    private Integer serviceTicketId;
    private Integer staffId;
    private String roleInTicket;
    private Instant assignedAt;
    private Boolean isPrimary;
    private String status;
    private String note;
}
