package com.g42.platform.gms.service_ticket_management.domain.entity;

import lombok.Data;

/**
 * Domain entity for ticket-specific custom inspection categories.
 */
@Data
public class TicketCustomCategory {
    private Integer id;
    private Integer inspectionId;
    private String categoryName;
    private Integer displayOrder;
    private String status = "ACTIVE";
}
