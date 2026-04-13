package com.g42.platform.gms.warehouse.api.dto;

import com.g42.platform.gms.common.enums.WarehouseTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDto {
    private Integer warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private WarehouseTypeEnum warehouseType;
    private Integer parentWarehouseId;
    private String address;
    private Integer managerStaffId;
    private Boolean isActive;
    private Instant createdAt;
}
