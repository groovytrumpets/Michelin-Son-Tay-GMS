package com.g42.platform.gms.warehouse.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReturnEntryItemRequest {

    @NotNull
    private Integer itemId;

    @NotNull
    @Min(1)
    private Integer quantity;

    /** Mô tả lỗi — bắt buộc với hàng trả, optional với hàng đổi mới */
    private String conditionNote;
}
