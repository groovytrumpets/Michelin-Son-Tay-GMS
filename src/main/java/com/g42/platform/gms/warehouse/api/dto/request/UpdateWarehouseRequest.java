package com.g42.platform.gms.warehouse.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateWarehouseRequest {

    @Size(max = 100, message = "Tên kho tối đa 100 ký tự")
    private String warehouseName;

    private String address;

    private Integer managerStaffId;

    /**
     * Đổi kho cha (null = giữ nguyên). Không được chọn chính nó hoặc kho con cháu của nó,
     * không được chọn kho lỗi; kho tổng (MASTER) luôn là gốc nên không có kho cha.
     */
    private Integer parentWarehouseId;
}
