package com.g42.platform.gms.warehouse.api.dto.request;

import com.g42.platform.gms.common.enums.WarehouseTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateWarehouseRequest {

    @NotBlank(message = "Mã kho không được để trống")
    @Size(max = 20, message = "Mã kho tối đa 20 ký tự")
    private String warehouseCode;

    @NotBlank(message = "Tên kho không được để trống")
    @Size(max = 100, message = "Tên kho tối đa 100 ký tự")
    private String warehouseName;

    @NotNull(message = "Loại kho không được để trống")
    private WarehouseTypeEnum warehouseType;

    /** ID kho cha — bắt buộc khi warehouseType = BRANCH hoặc DEFECTIVE */
    private Integer parentWarehouseId;

    private String address;

    private Integer managerStaffId;
}
