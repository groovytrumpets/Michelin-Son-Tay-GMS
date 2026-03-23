package com.g42.platform.gms.service_ticket_management.domain.repository;

import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspectionItem;
import com.g42.platform.gms.service_ticket_management.domain.projection.SafetyInspectionItemWithCategory;

import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for SafetyInspectionItem.
 */
public interface SafetyInspectionItemRepo {

    List<SafetyInspectionItem> findByInspectionId(Integer inspectionId);

    Optional<SafetyInspectionItem> findByInspectionIdAndWorkCategoryId(Integer inspectionId, Integer workCategoryId);

    Optional<SafetyInspectionItem> findByInspectionIdAndCustomCategoryId(Integer inspectionId, Integer customCategoryId);

    List<SafetyInspectionItemWithCategory> findByInspectionIdWithCategory(Integer inspectionId);

    /** Returns items with categoryName populated from JOIN query. */
    List<SafetyInspectionItem> findItemsWithCategory(Integer inspectionId);

    SafetyInspectionItem save(SafetyInspectionItem item);

    void deleteAll(List<SafetyInspectionItem> items);

    void delete(SafetyInspectionItem item);
}
