package com.g42.platform.gms.service_ticket_management.domain.repository;

import com.g42.platform.gms.service_ticket_management.domain.entity.WorkCategory;

import java.util.List;

public interface WorkCategoryRepo {

    /** Tên các đầu mục kiểm tra an toàn đang dùng, theo thứ tự hiển thị. */
    List<String> findActiveCategoryNames();

    /** Các đầu mục kiểm tra an toàn đang dùng. */
    List<WorkCategory> findActiveCategories();
}
