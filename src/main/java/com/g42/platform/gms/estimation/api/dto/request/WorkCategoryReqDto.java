package com.g42.platform.gms.estimation.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dữ liệu tạo / cập nhật một hạng mục công việc từ màn cấu hình hệ thống.
 * Bỏ trống categoryCode thì hệ thống tự sinh từ tên.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkCategoryReqDto {
    private String categoryName;
    private String categoryCode;
    private Integer displayOrder;
    private Integer taxRuleId;
    private Boolean isDefault;
    private Boolean isActive;
}
