package com.g42.platform.gms.service_ticket_management.infrastructure.repository;

import com.g42.platform.gms.service_ticket_management.infrastructure.entity.OdometerHistoryJpa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA Repository for OdometerHistory entity.
 * 
 * Provides CRUD operations and custom query methods for odometer readings.
 */
@Repository
public interface OdometerHistoryRepository extends JpaRepository<OdometerHistoryJpa, Integer> {
    
    /**
     * Find all odometer readings for a specific vehicle, ordered by recorded date descending.
     * 
     * @param vehicleId the vehicle ID
     * @return list of odometer readings
     */
    List<OdometerHistoryJpa> findByVehicleIdOrderByRecordedAtDesc(Integer vehicleId);
    
    /**
     * Find the latest odometer reading for a specific vehicle.
     * 
     * @param vehicleId the vehicle ID
     * @return Optional containing the latest reading if found
     */
    @Query("SELECT o FROM OdometerHistoryJpa o WHERE o.vehicleId = :vehicleId ORDER BY o.recordedAt DESC LIMIT 1")
    Optional<OdometerHistoryJpa> findLatestByVehicleId(@Param("vehicleId") Integer vehicleId);
    
    /**
     * Find all odometer readings for a specific service ticket.
     * 
     * @param serviceTicketId the service ticket ID
     * @return list of odometer readings
     */
    List<OdometerHistoryJpa> findByServiceTicketId(Integer serviceTicketId);
    
    /**
     * Find all odometer readings where rollback was detected.
     * 
     * @return list of readings with rollback detected
     */
    List<OdometerHistoryJpa> findByRollbackDetectedTrue();
}
