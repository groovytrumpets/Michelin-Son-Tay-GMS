package com.g42.platform.gms.warehouse.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Một nhân viên trong ô chọn "Quản lý kho" ở /warehouse-config. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseManagerOptionDto {
    private Integer staffId;
    private String fullName;
    private String phone;
    private String position;
}
