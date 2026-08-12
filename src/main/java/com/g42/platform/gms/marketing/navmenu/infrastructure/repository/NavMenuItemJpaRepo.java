package com.g42.platform.gms.marketing.navmenu.infrastructure.repository;

import com.g42.platform.gms.marketing.navmenu.infrastructure.entity.NavMenuItemJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NavMenuItemJpaRepo extends JpaRepository<NavMenuItemJpa, Integer> {

    /**
     * Lấy toàn bộ mục của một vị trí trong đúng một truy vấn rồi dựng cây ở tầng
     * ứng dụng — rẻ hơn nhiều so với đệ quy xuống database cho mỗi nhánh.
     */
    @Query("""
            SELECT n FROM NavMenuItemJpa n
            WHERE n.locationCode = :locationCode
            ORDER BY n.columnIndex ASC, n.displayOrder ASC, n.navItemId ASC
            """)
    List<NavMenuItemJpa> findAllForLocation(@Param("locationCode") String locationCode);

    List<NavMenuItemJpa> findByParentIsNullAndLocationCodeOrderByDisplayOrderAsc(String locationCode);

    /** Thứ tự lớn nhất hiện có, dùng để thêm mục mới vào cuối danh sách. */
    @Query("""
            SELECT COALESCE(MAX(n.displayOrder), 0) FROM NavMenuItemJpa n
            WHERE n.locationCode = :locationCode
              AND ((:parentId IS NULL AND n.parent IS NULL) OR n.parent.navItemId = :parentId)
            """)
    int findMaxDisplayOrder(@Param("locationCode") String locationCode, @Param("parentId") Integer parentId);
}
