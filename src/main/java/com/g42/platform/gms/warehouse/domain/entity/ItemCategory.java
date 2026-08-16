package com.g42.platform.gms.warehouse.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Danh mục phụ tùng / dịch vụ. Một phụ tùng có thể không thuộc danh mục nào.
 *
 * Xem {@link com.g42.platform.gms.warehouse.infrastructure.entity.ItemCategoryJpa}
 * để biết vì sao khái niệm này được tách khỏi work_category.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemCategory {

    private Integer itemCategoryId;
    private String categoryCode;
    private String categoryName;
    private String categoryType;
    private Integer displayOrder;
    private Boolean isActive;
    private Integer taxRuleId;
}
