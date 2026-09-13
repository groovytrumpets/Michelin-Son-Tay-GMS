package com.g42.platform.gms.warehouse.app.service.dto;



import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StockShortageInfo {
    private Integer warehouseId;
    private Integer itemId;
    private BigDecimal requested;
    private BigDecimal available;
}
