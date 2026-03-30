package com.g42.platform.gms.service_ticket_management.infrastructure.specification;

import com.g42.platform.gms.service_ticket_management.domain.enums.RoleInTicket;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Specification class for building dynamic queries for work history feature.
 * 
 * This class provides static methods to create JPA Specifications that can be
 * combined to filter service tickets for technician work history queries.
 * 
 * Usage example:
 * <pre>
 * Specification<ServiceTicketJpa> spec = Specification.where(null)
 *     .and(WorkHistorySpecification.byTechnicianId(technicianId))
 *     .and(WorkHistorySpecification.isCompleted())
 *     .and(WorkHistorySpecification.completedBetween(startDate, endDate));
 * </pre>
 */
public class WorkHistorySpecification {
    
    /**
     * Filter service tickets by technician ID.
     * 
     * This specification joins with the service_ticket_assignment table
     * and filters for records where:
     * - staff_id matches the given technicianId
     * - role_in_ticket is TECHNICIAN
     * 
     * This ensures that only service tickets assigned to the specific
     * technician are returned, providing data isolation for security.
     * 
     * @param technicianId The ID of the technician
     * @return Specification that filters by technician ID and role
     */
    public static Specification<ServiceTicketJpa> byTechnicianId(Integer technicianId) {
        return (root, query, cb) -> {
            // Join with service_ticket_assignment table
            Join<ServiceTicketJpa, ServiceTicketAssignmentJpa> assignmentJoin = 
                root.join("assignments", JoinType.INNER);
            
            // Filter: staff_id = technicianId AND role_in_ticket = 'TECHNICIAN'
            return cb.and(
                cb.equal(assignmentJoin.get("staffId"), technicianId),
                cb.equal(assignmentJoin.get("roleInTicket"), RoleInTicket.TECHNICIAN)
            );
        };
    }
    
    /**
     * Filter service tickets by COMPLETED or PAID status.
     * PAID là trạng thái cuối sau COMPLETED (lễ tân xác nhận thanh toán),
     * nên work history cần include cả hai.
     */
    public static Specification<ServiceTicketJpa> isCompleted() {
        return (root, query, cb) ->
            root.get("ticketStatus").in(TicketStatus.COMPLETED, TicketStatus.PAID);
    }
    
    /**
     * Filter service tickets by completion date range.
     * 
     * This specification filters for service tickets where the
     * completed_at timestamp falls within the specified date range.
     * The range is inclusive:
     * - Start: startDate at 00:00:00
     * - End: endDate at 23:59:59
     * 
     * @param startDate The start date of the range (inclusive)
     * @param endDate The end date of the range (inclusive)
     * @return Specification that filters by completion date range
     */
    public static Specification<ServiceTicketJpa> completedBetween(
        LocalDate startDate, 
        LocalDate endDate) {
        
        return (root, query, cb) -> {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            
            return cb.between(
                root.get("completedAt"), 
                startDateTime, 
                endDateTime
            );
        };
    }
    
    /**
     * Filter service tickets by vehicle license plate.
     * 
     * This specification joins with the vehicle table and filters
     * for records where the license plate matches the given value.
     * The comparison is case-insensitive.
     * 
     * @param licensePlate The license plate to search for
     * @return Specification that filters by license plate (case-insensitive)
     */
    public static Specification<ServiceTicketJpa> byLicensePlate(String licensePlate) {
        return (root, query, cb) -> {
            // Join with vehicle table
            Join<ServiceTicketJpa, Vehicle> vehicleJoin = 
                root.join("vehicleId", JoinType.INNER);
            
            // Filter: license_plate = licensePlate (case-insensitive)
            return cb.equal(
                cb.lower(vehicleJoin.get("licensePlate")), 
                licensePlate.toLowerCase()
            );
        };
    }
}
