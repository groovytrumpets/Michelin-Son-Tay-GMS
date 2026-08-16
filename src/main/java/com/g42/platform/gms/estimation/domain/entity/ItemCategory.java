package com.g42.platform.gms.estimation.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Danh mục phụ tùng / dịch vụ, nhìn từ phía báo giá.
 *
 * Trước changeset 014 đây là "hạng mục công việc" (work_category) — dùng chung
 * với các đầu mục kiểm tra an toàn và tự sinh thêm bản ghi mỗi khi advisor gõ một
 * tên hạng mục chưa có. Nay dòng báo giá chỉ tham chiếu danh mục có sẵn, còn tên
 * nhóm gõ tay được lưu thẳng vào {@code EstimateItem.categoryLabel}.
 *
 * Bản cấu hình đầy đủ của danh mục nằm ở module warehouse (ItemCategoryService);
 * ở đây chỉ cần đọc để nhóm dòng và lấy thuế mặc định.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemCategory {

    private Integer id;
    private String categoryCode;
    private String categoryName;
    private String categoryType;
    private Integer displayOrder;
    private Boolean isActive;
    private Integer taxRuleId;
}
