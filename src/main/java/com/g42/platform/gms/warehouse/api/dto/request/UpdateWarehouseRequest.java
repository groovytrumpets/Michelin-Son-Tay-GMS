package com.g42.platform.gms.warehouse.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateWarehouseRequest {

    @Size(max = 100, message = "Tên kho tối đa 100 ký tự")
    private String warehouseName;

    private String address;

    private Integer managerStaffId;
}
