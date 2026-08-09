package com.g42.platform.gms.estimation.api.dto.request;

import com.g42.platform.gms.estimation.domain.enums.EstimateTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstimateRequestDto {
    private Integer serviceTicketId;
    private EstimateTypeEnum estimateType;
    private Integer fallbackPricingConfigId;
    /** Hệ số markup gõ tay cho phiếu; có giá trị thì ưu tiên hơn fallbackPricingConfigId. */
    private java.math.BigDecimal manualMarkupMultiplier;
    private List<EstimateItemReqDto> items;

}
