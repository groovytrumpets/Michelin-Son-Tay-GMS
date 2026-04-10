package com.g42.platform.gms.warehouse.api.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class StockEntryImportResponse {
    /** Phiếu nhập đã tạo */
    private StockEntryResponse entry;
    /** Số dòng import thành công */
    private int importedCount;
    /** Danh sách lỗi từng dòng (nếu có) */
    private List<String> errors;
    /** Có lỗi không */
    private boolean hasErrors;
}
