package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.domain.enums.PhotoCategory;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.VehicleConditionPhotoJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA Repository for VehicleConditionPhoto entity.
 * 
 * Provides CRUD operations and custom query methods for vehicle condition photos.
 */
@Repository
public interface VehicleConditionPhotoRepository extends JpaRepository<VehicleConditionPhotoJpa, Integer> {
    
    /**
     * Find all photos for a specific service ticket.
     * 
     * @param serviceTicketId the service ticket ID
     * @return list of photos
     */
    List<VehicleConditionPhotoJpa> findByServiceTicketId(Integer serviceTicketId);
    
    /**
     * Find all photos for a specific service ticket and category.
     * 
     * @param serviceTicketId the service ticket ID
     * @param category the photo category
     * @return list of photos
     */
    List<VehicleConditionPhotoJpa> findByServiceTicketIdAndCategory(Integer serviceTicketId, PhotoCategory category);
    
    /**
     * Check if a service ticket has photos for a specific category.
     * 
     * @param serviceTicketId the service ticket ID
     * @param category the photo category
     * @return true if photos exist, false otherwise
     */
    boolean existsByServiceTicketIdAndCategory(Integer serviceTicketId, PhotoCategory category);
    
    /**
     * Get distinct photo categories for a service ticket.
     * 
     * @param serviceTicketId the service ticket ID
     * @return list of distinct categories
     */
    @Query("SELECT DISTINCT p.category FROM VehicleConditionPhotoJpa p WHERE p.serviceTicketId = :serviceTicketId")
    List<PhotoCategory> findDistinctCategoriesByServiceTicketId(@Param("serviceTicketId") Integer serviceTicketId);
}
