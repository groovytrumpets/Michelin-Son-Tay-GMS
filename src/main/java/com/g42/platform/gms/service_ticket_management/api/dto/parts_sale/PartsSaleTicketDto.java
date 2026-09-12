package com.g42.platform.gms.service_ticket_management.api.dto.parts_sale;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Ket qua thao tac tren phieu ban linh kien.
 *
 * - Buoc giu hang: phieu HOLDING, hang da RESERVED, billId/finalAmount con null.
 * - Buoc chot: phieu COMPLETED, bao gia ARCHIVED, bill san sang thu tien.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PartsSaleTicketDto {
    private Integer serviceTicketId;
    private String ticketCode;
    private Integer customerId;
    private Integer estimateId;
    private Integer billId;
    private BigDecimal finalAmount;
    /** HOLDING (dang giu hang) hoac COMPLETED (da chot, cho thu tien) */
    private String ticketStatus;
}
