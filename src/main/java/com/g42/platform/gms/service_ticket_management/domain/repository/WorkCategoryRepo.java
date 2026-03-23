package com.g42.platform.gms.service_ticket_management.domain.repository;

import com.g42.platform.gms.service_ticket_management.domain.entity.WorkCategory;

import java.util.List;

public interface WorkCategoryRepo {

    List<String> findDefaultWorkCategoryNames();

    /** Returns all active default categories as domain entities. */
    List<WorkCategory> findDefaultCategories();

    /** Returns all active categories as domain entities. */
    List<WorkCategory> findActiveCategories();
}
