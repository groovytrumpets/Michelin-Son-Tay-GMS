package com.g42.platform.gms.estimation.domain.repository;

import com.g42.platform.gms.estimation.domain.entity.WorkCategory;
import com.g42.platform.gms.estimation.infrastructure.entity.WorkCategoryJpa;


import java.util.List;

public interface WorkCategoryRepository {
    List<WorkCategory> findAllById(Iterable<Integer> workCategoryId);

    WorkCategory save(WorkCategory newCategory);

    int findMaxDisplayOrder();

    WorkCategory findById(Integer categoryId);

    /** Chỉ các hạng mục đang hoạt động, đã sắp theo thứ tự hiển thị. */
    List<WorkCategory> findAll();

    /** Tra theo tên, không phân biệt hoa thường; null nếu chưa có. */
    WorkCategory findByCategoryName(String categoryName);

    /** Gồm cả hạng mục đã ẩn, dùng cho màn cấu hình. */
    List<WorkCategory> findAllIncludingInactive();

    void deleteById(Integer categoryId);
}
