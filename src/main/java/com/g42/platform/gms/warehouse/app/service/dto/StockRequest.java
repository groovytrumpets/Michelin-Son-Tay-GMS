package com.g42.platform.gms.warehouse.app.service.dto;


import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockRequest {
    private Integer warehouseId;
    private Integer itemId;
    private BigDecimal quantity;
}
