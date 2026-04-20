package com.g42.platform.gms.warehouse.api.dto.entry;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CreateStockEntryRequest {

    @NotNull
    private Integer warehouseId;

    @NotBlank
    private String supplierName;

    /** Ngày nhập — mặc định hôm nay nếu không truyền */
    private LocalDate entryDate = LocalDate.now();

    private String notes;

    @NotEmpty
    @Valid
    private List<StockEntryItemRequest> items;
}
