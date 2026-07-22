package com.g42.platform.gms.promotion.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromotionBuyGiftItemDto {
    private Integer catalogItemId;
    private Integer quantity;
}
