package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyWorkCategoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkCategoryRepository extends JpaRepository<SafetyWorkCategoryJpa, Integer> {
    
    /**
     * Find all active work categories ordered by display order
     * Note: All categories in work_category table (IDs 9001-9013) are safety inspection items
     */
    @Query("SELECT w FROM SafetyWorkCategoryJpa w WHERE w.isActive = true ORDER BY w.displayOrder ASC")
    List<SafetyWorkCategoryJpa> findActiveCategories();
    
    /**
     * Find active safety inspection category names only
     */
    @Query("SELECT w.categoryName FROM SafetyWorkCategoryJpa w WHERE w.isActive = true ORDER BY w.displayOrder ASC")
    List<String> findActiveSafetyInspectionCategoryNames();
    
    /**
     * Find all active work categories ordered by display order
     */
    @Query("SELECT w FROM SafetyWorkCategoryJpa w WHERE w.isActive = true ORDER BY w.displayOrder ASC")
    List<SafetyWorkCategoryJpa> findActiveWorkCategoriesOrderByDisplayOrder();
}