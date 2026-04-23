package com.g42.platform.gms.marketing.service_combo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ComboCreateDto {
    private Integer comboId;
    private Integer includedItemId;
    private Integer quantity;
}
