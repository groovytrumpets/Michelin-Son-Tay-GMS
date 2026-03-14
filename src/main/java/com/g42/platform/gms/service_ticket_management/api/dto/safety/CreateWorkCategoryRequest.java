package com.g42.platform.gms.service_ticket_management.api.dto.safety;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO để tạo mới một hạng mục kiểm tra an toàn (work_category).
 * Hạng mục mới sẽ có is_default = false, is_active = true.
 */
@Data
public class CreateWorkCategoryRequest {

    @NotBlank(message = "Tên hạng mục là bắt buộc")
    @Size(max = 100, message = "Tên hạng mục tối đa 100 ký tự")
    private String categoryName;

    @Size(max = 50, message = "Mã hạng mục tối đa 50 ký tự")
    private String categoryCode;

    private Integer displayOrder;

    private String advisorNote;
}
