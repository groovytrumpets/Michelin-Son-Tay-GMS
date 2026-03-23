package com.g42.platform.gms.service_ticket_management.domain.repository;

import com.g42.platform.gms.service_ticket_management.domain.entity.TicketCustomCategory;

/**
 * Domain repository interface for TicketCustomCategory.
 */
public interface TicketCustomCategoryRepo {

    boolean existsByInspectionIdAndCategoryName(Integer inspectionId, String categoryName);

    TicketCustomCategory save(TicketCustomCategory customCategory);
}
