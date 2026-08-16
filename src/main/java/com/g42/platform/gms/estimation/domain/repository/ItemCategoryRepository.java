package com.g42.platform.gms.estimation.domain.repository;

import com.g42.platform.gms.estimation.domain.entity.ItemCategory;

import java.util.List;

/**
 * Chỉ đọc — báo giá không còn được phép tự tạo danh mục mới. Muốn thêm danh mục
 * thì vào màn Hệ thống → Danh mục phụ tùng & dịch vụ.
 */
public interface ItemCategoryRepository {

    List<ItemCategory> findAllById(Iterable<Integer> itemCategoryIds);

    ItemCategory findById(Integer categoryId);

    /** Danh mục đang dùng, đã sắp theo thứ tự hiển thị. */
    List<ItemCategory> findAll();

    ItemCategory findByCategoryName(String categoryName);
}
