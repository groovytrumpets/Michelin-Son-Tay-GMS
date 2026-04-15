package com.g42.platform.gms.estimation.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EstimateViaAllocationDto {
    EstimateItemDto  estimateItemDto;
    StockAllocationDto stockAllocationDto;
}
