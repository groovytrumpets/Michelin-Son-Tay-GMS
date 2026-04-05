package com.g42.platform.gms.warehouse.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpecificationRespondDto {
    private String attributeCode; // vd: "WIDTH"
    private String displayName;   // vd: "Chiều rộng"
    private String specValue;     // vd: "225"
    private String unit;
}
