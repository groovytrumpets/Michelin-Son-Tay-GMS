package com.g42.platform.gms.warehouse.domain.entity;

import com.g42.platform.gms.common.enums.WarehouseTypeEnum;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Warehouse {

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