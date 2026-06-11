package com.g42.platform.gms.warehouse.api.dto.request;

import com.g42.platform.gms.warehouse.domain.enums.ReturnType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Request DTO để tạo phiếu hoàn từ phiếu xuất đã xác nhận.
 * Đảm bảo phiếu hoàn luôn có đầy đủ context: phiếu xuất nguồn, allocation, phiếu dịch vụ.
 */
@Data
public class CreateReturnEntryFromIssueRequest {

    @NotNull(message = "issueId không được null")
    @Min(value = 1, message = "issueId phải > 0")
    private Integer issueId;

    @NotNull(message = "returnReason không được null")
    private String returnReason;

    private ReturnType returnType = ReturnType.CUSTOMER_RETURN;

    @Valid
    @NotNull(message = "items không được null")
    private List<ReturnItemFromIssue> items;

    @Valid
    private List<ReturnItemFromIssue> exchangeItems;

    @Data
    public static class ReturnItemFromIssue {
        /**
         * allocationId từ phiếu xuất (REQUIRED)
         * Backend sẽ tự động resolve: issueItemId, entryItemId, itemId
         */
        @NotNull(message = "allocationId không được null")
        private Integer allocationId;

        @NotNull(message = "quantity không được null")
        @Min(value = 1, message = "quantity phải >= 1")
        private Integer quantity;

        private String conditionNote;

        /**
         * WRONG_TYPE hoặc DEFECTIVE
         */
        private com.g42.platform.gms.warehouse.domain.enums.ReturnReason returnReason;

        /**
         * Required if returnReason = DEFECTIVE
         */
        private com.g42.platform.gms.warehouse.domain.enums.DefectCause defectCause;

        /**
         * Required if defectCause = TECHNICIAN or WAREHOUSE
         */
        private Integer responsibleStaffId;
    }
}
