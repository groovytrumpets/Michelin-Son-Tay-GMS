package com.g42.platform.gms.warehouse.api.dto.request;

import lombok.Data;

import java.util.List;

/**
 * Xếp một loạt phụ tùng / dịch vụ vào cùng một danh mục.
 *
 * {@code itemCategoryId} để trống nghĩa là GỠ danh mục khỏi các món được chọn —
 * hàng hóa không thuộc danh mục nào là trạng thái hợp lệ.
 */
@Data
public class AssignItemCategoryRequest {
    private Integer itemCategoryId;
    private List<Integer> itemIds;
}
