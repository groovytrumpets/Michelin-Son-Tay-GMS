package com.g42.platform.gms.warehouse.api.dto.request;

import com.g42.platform.gms.warehouse.domain.enums.DefectCause;
import com.g42.platform.gms.warehouse.domain.enums.ReturnReason;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReturnEntryItemRequest {

    @NotNull
    private Integer itemId;

    private Integer allocationId;

    private Integer sourceIssueId;

    private Integer sourceIssueItemId;

    private Integer entryItemId;

    @NotNull
    @Min(1)
    private Integer quantity;

    /** Mô tả tình trạng sản phẩm khi trả, bắt buộc với hàng trả */
    private String conditionNote;

    /**
     * Phân loại lý do hoàn:
     * WRONG_TYPE – xuất nhầm kiểu/mẫu, hàng còn nguyên vẹn → trả về kho thường
     * DEFECTIVE  – hàng bị lỗi → trả vào kho hàng lỗi
     * Mặc định WRONG_TYPE nếu không truyền.
     */
    private ReturnReason returnReason = ReturnReason.WRONG_TYPE;

    /**
     * Nguyên nhân gây lỗi – bắt buộc khi returnReason = DEFECTIVE.
     * TECHNICIAN / WAREHOUSE / SUPPLIER
     */
    private DefectCause defectCause;

    /**
     * Nhân viên chịu trách nhiệm – bắt buộc khi defectCause = TECHNICIAN hoặc WAREHOUSE.
     * Null khi lỗi từ SUPPLIER.
     */
    private Integer responsibleStaffId;
}
