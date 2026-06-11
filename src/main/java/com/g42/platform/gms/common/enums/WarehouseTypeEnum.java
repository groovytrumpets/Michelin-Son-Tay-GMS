package com.g42.platform.gms.common.enums;

public enum WarehouseTypeEnum {
    //enum('MASTER', 'BRANCH', 'DEFECTIVE')
    MASTER,
    BRANCH,
    /** Kho chứa hàng lỗi – mỗi chi nhánh có 1 kho loại này, linked qua parentWarehouseId */
    DEFECTIVE,
}
