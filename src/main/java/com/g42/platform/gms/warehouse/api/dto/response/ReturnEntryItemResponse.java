package com.g42.platform.gms.warehouse.api.dto.response;

import com.g42.platform.gms.warehouse.domain.enums.DefectCause;
import com.g42.platform.gms.warehouse.domain.enums.ReturnReason;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReturnEntryItemResponse {
    private Integer returnItemId;
    private Integer itemId;
    private String itemCode;
    private String itemName;
    private Integer allocationId;
    private Integer sourceIssueItemId;
    private Integer entryItemId;
    private String entryCode;
    private String entryLotCode;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String conditionNote;
    private List<String> attachmentUrls;

    /** Phân loại lý do hoàn: WRONG_TYPE hoặc DEFECTIVE */
    private ReturnReason returnReason;

    /** Nguyên nhân lỗi – chỉ có giá trị khi returnReason = DEFECTIVE */
    private DefectCause defectCause;

    /** Tên nhân viên chịu trách nhiệm */
    private Integer responsibleStaffId;
    private String responsibleStaffName;

    /** Kho hàng lỗi đích đã nhận hàng (sau khi confirm) */
    private Integer defectiveWarehouseId;
    private String defectiveWarehouseName;
}
