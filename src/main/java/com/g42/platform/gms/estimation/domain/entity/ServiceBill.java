package com.g42.platform.gms.estimation.domain.entity;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBill {
    private Integer billId;
    private ServiceTicketJpa serviceTicket;
    private BigDecimal subTotal;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String paymentMethod;
    private String paymentStatus;
    private Instant paidAt;
    private String billStatus;
    private Integer warehouseId;
    private Integer version;
    private Integer estimateId;
}
