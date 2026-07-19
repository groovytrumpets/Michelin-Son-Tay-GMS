package com.g42.platform.gms.service_ticket_management.api.dto.manage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một dòng phụ tùng/dịch vụ khách đã sử dụng, kèm thông tin phiếu dịch vụ nguồn
 * để FE mở lại phiếu (ticketCode) khi cần.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsedItemHistoryResponse {
    private Integer serviceTicketId;
    private String ticketCode;
    private LocalDateTime receivedAt;
    private String itemName;
    private String categoryName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal finalPrice;
    private Boolean isGift;
}
