package com.g42.platform.gms.estimation.api.dto;

import com.g42.platform.gms.common.enums.EstimateEnum;
import com.g42.platform.gms.estimation.domain.enums.EstimateTypeEnum;
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
public class EstimateRespondDto {
    private Integer estimateId;
    private Integer serviceTicketId;
    private EstimateTypeEnum estimateType;
    private EstimateEnum status;
    private Instant createdAt;
    private Instant approvedAt;
    private Integer version;
    private Integer revisedFromId;
    private BigDecimal subTotal;
    private BigDecimal totalTaxAmount;
    private BigDecimal totalPrice;
    List<EstimateItemDto> items;
}
