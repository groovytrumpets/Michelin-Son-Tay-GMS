package com.g42.platform.gms.warehouse.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReturnEntryItemRequest {

    @NotNull
    private Integer itemId;

    /** Dòng allocation tương ứng, dùng để trả đúng sản phẩm đang được báo giá/giữ hàng */
    private Integer allocationId;

    /** (Legacy) Dòng issue gốc tương ứng, không còn dùng cho màn hình mới */
    private Integer sourceIssueItemId;

    /** Tùy chọn: chỉ định lô đích (entryItemId) nếu muốn ghi đích trả vào lô cụ thể */
    private Integer entryItemId;

    @NotNull
    @Min(1)
    private Integer quantity;

    /** Mô tả lỗi — bắt buộc với hàng trả, optional với hàng đổi mới */
    private String conditionNote;
}
