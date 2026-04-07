package com.g42.platform.gms.warehouse.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReturnEntryItemRequest {

    @NotNull
    private Integer itemId;

    @NotNull
    @Min(1)
    private Integer quantity;

    @NotBlank
    private String conditionNote;
}
