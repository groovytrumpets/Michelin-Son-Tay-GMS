package com.g42.platform.gms.estimation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Một dòng phụ tùng/dịch vụ (estimate item) đã dùng, gắn với serviceTicketId nguồn.
 * Dùng cho API tổng hợp lịch sử phụ tùng/dịch vụ đã sử dụng theo khách hàng.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsedEstimateItemDto {
    private Integer serviceTicketId;
    private String itemName;
    private String categoryName;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal finalPrice;
    private Boolean isGift;
}
