package com.g42.platform.gms.warehouse.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Danh mục phụ tùng / dịch vụ cho trang công khai (/services, /parts...).
 * Tách khỏi ItemCategoryDto để không lộ cấu hình nội bộ như thuế mặc định ra ngoài.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PublicItemCategoryDto {
    private Integer itemCategoryId;
    private String categoryCode;
    private String categoryName;
    /** PART, SERVICE hoặc null (dùng chung cho cả hai). */
    private String categoryType;
    private Integer displayOrder;
}
