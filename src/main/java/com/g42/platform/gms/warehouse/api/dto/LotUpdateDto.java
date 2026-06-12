package com.g42.platform.gms.warehouse.api.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class LotUpdateDto {
    private Integer entryItemId;
    private Integer remainingQuantity;
    private BigDecimal sellingPrice;
}
