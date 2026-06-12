package com.g42.platform.gms.warehouse.api.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class WarehouseUpdateDto {
    private Integer warehouseId;
    private Integer quantity;
    private Integer reservedQuantity;
    private BigDecimal sellingPrice;
    private List<LotUpdateDto> lots;
}
