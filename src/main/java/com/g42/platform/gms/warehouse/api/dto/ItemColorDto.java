package com.g42.platform.gms.warehouse.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemColorDto {
    private Integer itemColorId;
    private Integer itemId;
    private String colorCode;
    private String colorName;
    private Integer displayOrder;
}
