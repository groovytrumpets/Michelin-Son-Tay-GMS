package com.g42.platform.gms.billing.api.dto;

import com.g42.platform.gms.estimation.api.dto.EstimateRespondDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServiceBillCreateDto {
    private Integer serviceTicketId;
//    private BigDecimal discountAmount;
    private String paymentStatus;
    private Instant paidAt;
    private Integer warehouseId;
    private Integer estimateId;
//    private Integer promotionId;
}
