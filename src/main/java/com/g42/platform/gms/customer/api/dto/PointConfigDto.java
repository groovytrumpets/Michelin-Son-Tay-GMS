package com.g42.platform.gms.customer.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PointConfigDto {
    private Integer pointsPer1000Vnd;
    private Integer bonusPointsPerService;
    private Integer pointsPerReferral;
}
