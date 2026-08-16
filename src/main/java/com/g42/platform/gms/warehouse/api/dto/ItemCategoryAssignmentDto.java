package com.g42.platform.gms.warehouse.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Một dòng trong màn xếp danh mục: đủ để nhận ra món hàng và biết nó đang thuộc
 * danh mục nào, không kéo theo giá / tồn kho cho nhẹ.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemCategoryAssignmentDto {
    private Integer itemId;
    private String itemName;
    private String sku;
    private String partNumber;
    /** PART / SERVICE / COMBO — để màn hình tách phụ tùng với dịch vụ. */
    private String itemType;
    private Boolean isActive;
    private Integer itemCategoryId;
    private String categoryName;
}
