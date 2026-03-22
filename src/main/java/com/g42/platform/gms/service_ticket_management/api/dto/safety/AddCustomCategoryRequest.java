package com.g42.platform.gms.service_ticket_management.api.dto.safety;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request để thêm hạng mục kiểm tra tùy chỉnh vào một phiếu kiểm tra an toàn.
 * Hạng mục này được lưu vào ticket_custom_category (không ảnh hưởng work_category).
 */
@Data
public class AddCustomCategoryRequest {

    @NotBlank(message = "Tên hạng mục không được để trống")
    private String categoryName;

    private Integer displayOrder;
}
