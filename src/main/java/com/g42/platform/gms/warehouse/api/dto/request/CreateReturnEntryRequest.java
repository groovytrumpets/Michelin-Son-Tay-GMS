package com.g42.platform.gms.warehouse.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateReturnEntryRequest {

    @NotNull
    private Integer warehouseId;

    @NotBlank
    private String returnReason;

    /** Liên kết phiếu xuất gốc nếu có */
    private Integer sourceIssueId;

    @NotEmpty
    @Valid
    private List<ReturnEntryItemRequest> items;
}
