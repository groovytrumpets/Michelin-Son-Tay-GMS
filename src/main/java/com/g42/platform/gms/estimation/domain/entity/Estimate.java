package com.g42.platform.gms.estimation.domain.entity;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
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
public class Estimate {

    private Integer id;
    private ServiceTicketJpa serviceTicket;
    private String estimateType;
    private String status;
    private Instant createdAt;
    private Instant approvedAt;
    private Integer version;
    private Integer revisedFromId;
}
