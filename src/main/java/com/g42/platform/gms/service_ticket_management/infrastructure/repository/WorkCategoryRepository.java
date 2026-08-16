package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyWorkCategoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Đầu mục kiểm tra an toàn. Mọi bản ghi trong work_category đều là đầu mục kiểm tra
 * kể từ changeset 014, nên không còn lọc theo is_default nữa — trước đây bộ lọc đó
 * kéo nhầm cả danh mục phụ tùng ("Lốp ô tô", "Gạt mưa") vào phiếu kiểm tra.
 */
@Repository
public interface WorkCategoryRepository extends JpaRepository<SafetyWorkCategoryJpa, Integer> {

    /** Đầu mục đang dùng, theo đúng thứ tự hiển thị trên phiếu kiểm tra. */
    @Query("SELECT w FROM SafetyWorkCategoryJpa w WHERE w.isActive = true ORDER BY w.displayOrder ASC")
    List<SafetyWorkCategoryJpa> findActiveCategories();

    /** Chỉ lấy tên, dùng cho các màn chỉ cần hiển thị danh sách hạng mục. */
    @Query("SELECT w.categoryName FROM SafetyWorkCategoryJpa w WHERE w.isActive = true ORDER BY w.displayOrder ASC")
    List<String> findActiveSafetyInspectionCategoryNames();

    @Query("SELECT COALESCE(MAX(w.displayOrder), 0) FROM SafetyWorkCategoryJpa w")
    int findMaxDisplayOrder();

    boolean existsByCategoryName(String categoryName);

    boolean existsByCategoryCode(String categoryCode);

    SafetyWorkCategoryJpa findFirstByCategoryNameIgnoreCase(String categoryName);
}
