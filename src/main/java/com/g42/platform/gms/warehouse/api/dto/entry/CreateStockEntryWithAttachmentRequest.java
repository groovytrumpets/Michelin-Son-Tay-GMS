package com.g42.platform.gms.warehouse.api.dto.entry;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request cho endpoint tạo phiếu nhập kho + ảnh chứng từ trong 1 form.
 * Dùng @ModelAttribute (multipart/form-data).
 * items truyền dưới dạng JSON string, ví dụ:
 * [{"itemId":8,"quantity":50,"importPrice":45000,"markupMultiplier":1.5}]
 */
@Data
public class CreateStockEntryWithAttachmentRequest {

    @NotNull
    private Integer warehouseId;

    @NotBlank
    private String supplierName;

    /** Ngày nhập dạng yyyy-MM-dd, mặc định hôm nay nếu để trống */
    private String entryDate;

    private String notes;

    /**
     * JSON array string của items.
     * Ví dụ: [{"itemId":8,"quantity":50,"importPrice":45000,"markupMultiplier":1.5}]
     */
    @NotBlank
    private String items;

    /** Ảnh chứng từ — bắt buộc */
    @NotNull
    private MultipartFile file;
}
