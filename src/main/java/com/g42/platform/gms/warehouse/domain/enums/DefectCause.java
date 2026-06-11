package com.g42.platform.gms.warehouse.domain.enums;

/**
 * Nguyên nhân gây ra lỗi – chỉ áp dụng khi ReturnReason = DEFECTIVE.
 *
 * TECHNICIAN  – lỗi do kỹ thuật viên (lắp sai, làm hỏng trong quá trình thực hiện)
 * WAREHOUSE   – lỗi do kho (nhập sai lô, bảo quản kém, xuất nhầm)
 * SUPPLIER    – lỗi từ nhà cung cấp (sản phẩm có khuyết tật từ nơi sản xuất/nhập về)
 */
public enum DefectCause {
    TECHNICIAN,
    WAREHOUSE,
    SUPPLIER
}
