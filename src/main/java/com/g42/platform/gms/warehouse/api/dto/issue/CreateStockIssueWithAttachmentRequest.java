package com.g42.platform.gms.warehouse.api.dto.issue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request cho endpoint tạo phiếu xuất kho + ảnh chứng từ trong 1 form.
 * Dùng @ModelAttribute (multipart/form-data).
 * items truyền dưới dạng JSON string, ví dụ:
 * [{"itemId":8,"quantity":50,"discountRate":0}]
 */
@Data
public class CreateStockIssueWithAttachmentRequest {

    @NotNull
    private Integer warehouseId;

    @NotNull
    private String issueType; // "INDEPENDENT", "SERVICE_TICKET", "WARRANTY_CLAIM", etc.

    @NotBlank
    private String issueReason;

    /** Nếu issueType = SERVICE_TICKET */
    private Integer serviceTicketId;

    /**
     * JSON array string của items.
     * Ví dụ: [{"itemId":8,"quantity":50,"discountRate":0}]
     */
    @NotBlank
    private String items;

    /** Ảnh chứng từ — bắt buộc */
    @NotNull
    private MultipartFile file;
}
