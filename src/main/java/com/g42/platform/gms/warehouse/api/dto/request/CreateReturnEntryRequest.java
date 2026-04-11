package com.g42.platform.gms.warehouse.api.dto.request;

import com.g42.platform.gms.warehouse.domain.enums.ReturnType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CreateReturnEntryRequest {

    @NotNull
    private Integer warehouseId;

    @NotBlank
    private String returnReason;

    /**
     * Loại phiếu hoàn:
     * CUSTOMER_RETURN (default) — khách trả → cộng inventory
     * SUPPLIER_RETURN — trả NCC → trừ inventory
     * EXCHANGE — đổi hàng → cộng hàng lỗi + trừ hàng mới
     */
    private ReturnType returnType = ReturnType.CUSTOMER_RETURN;

    /** Liên kết phiếu xuất gốc nếu có */
    private Integer sourceIssueId;

    /** Danh sách hàng trả về */
    @NotEmpty
    @Valid
    private List<ReturnEntryItemRequest> items;

    /**
     * Danh sách hàng đổi mới — chỉ dùng khi returnType = EXCHANGE.
     * Mỗi item sẽ được trừ khỏi inventory.
     */
    @Valid
    private List<ReturnEntryItemRequest> exchangeItems = new ArrayList<>();
}
