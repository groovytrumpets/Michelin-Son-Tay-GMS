package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyInspectionItemJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.projection.SafetyInspectionItemWithCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SafetyInspectionItemRepository extends JpaRepository<SafetyInspectionItemJpa, Integer> {
    
    List<SafetyInspectionItemJpa> findByInspectionId(Integer inspectionId);
    
    /**
     * Find inspection items with category names using JOIN query.
     * Returns projection with categoryName populated from work_category table.
     */
    @Query(value = """
        SELECT 
            i.item_id AS itemId,
            i.inspection_id AS inspectionId,
            i.work_category_id AS workCategoryId,
            i.item_status AS itemStatus,
            i.notes AS notes,
            wc.category_name AS categoryName
        FROM safety_inspection_item i
        INNER JOIN work_category wc ON i.work_category_id = wc.idwork_category
        WHERE i.inspection_id = :inspectionId
        ORDER BY wc.display_order
        """, nativeQuery = true)
    List<SafetyInspectionItemWithCategory> findByInspectionIdWithCategory(@Param("inspectionId") Integer inspectionId);
    
    @Modifying
    @Query("DELETE FROM SafetyInspectionItemJpa i WHERE i.inspectionId = :inspectionId")
    void deleteByInspectionId(@Param("inspectionId") Integer inspectionId);
}
