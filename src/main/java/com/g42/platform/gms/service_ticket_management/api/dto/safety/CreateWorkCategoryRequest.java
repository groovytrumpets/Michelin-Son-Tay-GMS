package com.g42.platform.gms.service_ticket_management.api.dto.safety;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Tạo / sửa một đầu mục kiểm tra an toàn (bảng work_category).
 *
 * Đây KHÔNG phải danh mục phụ tùng — danh mục hàng hóa nằm ở item_category và có
 * màn cấu hình riêng.
 */
@Data
public class CreateWorkCategoryRequest {

    @NotBlank(message = "Tên hạng mục là bắt buộc")
    @Size(max = 100, message = "Tên hạng mục tối đa 100 ký tự")
    private String categoryName;

    @Size(max = 50, message = "Mã hạng mục tối đa 50 ký tự")
    private String categoryCode;

    private Integer displayOrder;

    /** Chỉ dùng khi cập nhật; tạo mới luôn đang hoạt động. */
    private Boolean isActive;
}
