package com.g42.platform.gms.warehouse.api.dto.request;

import com.g42.platform.gms.warehouse.api.dto.entry.StockEntryItemRequest;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/** Cập nhật phiếu nhập kho — chỉ khi DRAFT */
@Data
public class UpdateStockEntryRequest {
    private String supplierName;
    private LocalDate entryDate;
    private String notes;
    /** Nếu truyền thì replace toàn bộ items */
    private List<StockEntryItemRequest> items;
}
