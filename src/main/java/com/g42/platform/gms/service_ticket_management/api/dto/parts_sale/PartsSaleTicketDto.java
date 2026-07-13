package com.g42.platform.gms.service_ticket_management.api.dto.parts_sale;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Kết quả tạo phiếu bán linh kiện: phiếu đã COMPLETED, báo giá ARCHIVED,
 * hàng đã giữ (RESERVED) và bill sẵn sàng thanh toán.
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
}
