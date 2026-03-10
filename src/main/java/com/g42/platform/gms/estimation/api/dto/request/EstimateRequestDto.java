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
    private List<EstimateItemReqDto> items;

}
