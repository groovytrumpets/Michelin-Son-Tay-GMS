package com.g42.platform.gms.warehouse.domain.entity;

import com.g42.platform.gms.warehouse.domain.enums.DefectCause;
import com.g42.platform.gms.warehouse.domain.enums.ReturnReason;
import lombok.Data;

@Data
public class ReturnEntryItem {
    private Integer returnItemId;
    private Integer returnId;
    private Integer itemId;
    private Integer allocationId;
    private Integer sourceIssueItemId;
    private Integer entryItemId;
    private Integer quantity;
    private String conditionNote;
    private boolean exchangeItem;

    /**
     * Phân loại lý do hoàn từng dòng hàng.
     * WRONG_TYPE  → trả về kho thường (warehouseId của phiếu hoàn)
     * DEFECTIVE   → trả vào kho hàng lỗi của chi nhánh (xác định khi confirm)
     */
    private ReturnReason returnReason;

    /**
     * Nguyên nhân gây lỗi – bắt buộc khi returnReason = DEFECTIVE.
     * TECHNICIAN / WAREHOUSE / SUPPLIER
     */
    private DefectCause defectCause;

    /**
     * Nhân viên chịu trách nhiệm về lỗi.
     * Bắt buộc khi defectCause = TECHNICIAN hoặc WAREHOUSE.
     * Null khi lỗi do SUPPLIER.
     */
    private Integer responsibleStaffId;

    /**
     * Kho hàng lỗi đích nhận hàng khi returnReason = DEFECTIVE.
     * Được điền tự động khi confirm (tìm kho DEFECTIVE của chi nhánh).
     */
    private Integer defectiveWarehouseId;
}
