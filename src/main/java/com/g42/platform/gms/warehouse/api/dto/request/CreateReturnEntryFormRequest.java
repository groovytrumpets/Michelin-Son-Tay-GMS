package com.g42.platform.gms.warehouse.api.dto.request;

import com.g42.platform.gms.warehouse.domain.enums.ReturnType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Request tạo phiếu hoàn hàng + ảnh lỗi trong 1 form (multipart/form-data).
 * items: JSON string — hàng trả về.
 * exchangeItems: JSON string — hàng đổi mới (chỉ khi returnType = EXCHANGE).
 * files: danh sách ảnh lỗi, tất cả gắn vào item đầu tiên (1 item/phiếu hoàn từ UI).
 *        Nếu nhiều items thì dùng endpoint /items/{id}/attachments để upload thêm.
 */
@Data
public class CreateReturnEntryFormRequest {

    private Integer warehouseId;

    @NotBlank
    private String returnReason;

    /** CUSTOMER_RETURN | SUPPLIER_RETURN | EXCHANGE */
    private ReturnType returnType = ReturnType.CUSTOMER_RETURN;

    private Integer sourceIssueId;

    /** JSON array: [{"itemId":8,"quantity":5,"conditionNote":"Vỏ nứt"}] */
    @NotBlank
    private String items;

    /**
     * JSON array hàng đổi mới — chỉ dùng khi returnType = EXCHANGE.
     * [{"itemId":9,"quantity":5}]
     */
    private String exchangeItems;

    /** Nhiều ảnh lỗi — tất cả gắn vào item đầu tiên trong phiếu */
    private List<MultipartFile> files;

    // Backward compatible: vẫn nhận file_0..file_4 từ FE cũ
    private MultipartFile file_0;
    private MultipartFile file_1;
    private MultipartFile file_2;
    private MultipartFile file_3;
    private MultipartFile file_4;
}
