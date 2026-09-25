package com.g42.platform.gms.service_ticket_management.infrastructure.specification;

import com.g42.platform.gms.auth.entity.CustomerProfile;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketStatus;
import com.g42.platform.gms.service_ticket_management.domain.enums.TicketType;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketAssignmentJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.ServiceTicketJpa;
import com.g42.platform.gms.vehicle.entity.Vehicle;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Specification cho ServiceTicket để filter và search.
 * Tương tự BookingRequestSpecification trong booking_management.
 */
public class ServiceTicketSpecification {
    
    /**
     * Filter service tickets theo date, status.
     * 
     * @param date Filter theo received_at date
     * @param status Filter theo ticket status
     * @return Specification
     */
    public static Specification<ServiceTicketJpa> filter(LocalDate date, TicketStatus status, TicketType ticketType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter theo received_at date
            if (date != null) {
                LocalDateTime startOfDay = date.atStartOfDay();
                LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

                predicates.add(
                    cb.between(root.get("receivedAt"), startOfDay, endOfDay)
                );
            }

            // Filter theo status
            if (status != null) {
                predicates.add(
                    cb.equal(root.get("ticketStatus"), status)
                );
            }

            // Filter theo loại phiếu (Sửa xe / Bán hàng)
            if (ticketType != null) {
                predicates.add(
                    cb.equal(root.get("ticketType"), ticketType)
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * Lọc theo phiếu bán lẻ khách vãng lai (is_walk_in).
     *
     * Phiếu cũ tạo trước changeset 042 có cột này mặc định false nên không lọt vào
     * nhóm khách lẻ.
     *
     * @param walkIn true = chỉ phiếu khách lẻ, false = chỉ phiếu khách có hồ sơ
     */
    public static Specification<ServiceTicketJpa> byBranch(Integer branchId) {
        return (root, query, cb) -> cb.equal(root.get("branchId"), branchId);
    }

    public static Specification<ServiceTicketJpa> byWalkIn(boolean walkIn) {
        return (root, query, cb) -> walkIn
                ? cb.isTrue(root.get("isWalkIn"))
                : cb.or(cb.isFalse(root.get("isWalkIn")), cb.isNull(root.get("isWalkIn")));
    }

    /**
     * Filter service tickets theo staffId (qua bảng service_ticket_assignment).
     *
     * @param staffId ID của kỹ thuật viên
     * @return Specification
     */
    public static Specification<ServiceTicketJpa> assignedToStaff(Integer staffId) {
        return (root, query, cb) -> {
            Subquery<Integer> subquery = query.subquery(Integer.class);
            var assignmentRoot = subquery.from(ServiceTicketAssignmentJpa.class);
            subquery.select(assignmentRoot.get("serviceTicketId"))
                .where(
                    cb.and(
                        cb.equal(assignmentRoot.get("staffId"), staffId),
                        cb.notEqual(assignmentRoot.get("status"), com.g42.platform.gms.service_ticket_management.domain.enums.AssignmentStatus.CANCELLED)
                    )
                );
            return root.get("serviceTicketId").in(subquery);
        };
    }

    /**
     * Search service tickets theo keyword.
     * Tìm kiếm trong: ticket code, customer name, customer phone, license plate.
     * Sử dụng subquery để search trong customer và vehicle.
     * 
     * @param keyword Từ khóa tìm kiếm
     * @return Specification
     */
    public static Specification<ServiceTicketJpa> search(String keyword) {
        return (root, query, cb) -> {
            String like = "%" + keyword.toLowerCase() + "%";
            
            List<Predicate> predicates = new ArrayList<>();
            
            // === 1. SEARCH TRONG TICKET CODE ===
            predicates.add(cb.like(cb.lower(root.get("ticketCode")), like));
            
            // === 2. SEARCH TRONG CUSTOMER REQUEST ===
            predicates.add(cb.like(cb.lower(root.get("customerRequest")), like));
            
            // === 3. SEARCH TRONG CUSTOMER NAME (SUBQUERY) ===
            Subquery<Integer> customerNameSubquery = query.subquery(Integer.class);
            var customerRoot = customerNameSubquery.from(CustomerProfile.class);
            customerNameSubquery.select(customerRoot.get("customerId"))
                .where(cb.like(cb.lower(customerRoot.get("fullName")), like));
            predicates.add(root.get("customerId").in(customerNameSubquery));
            
            // === 4. SEARCH TRONG CUSTOMER PHONE (SUBQUERY) ===
            Subquery<Integer> customerPhoneSubquery = query.subquery(Integer.class);
            var customerRoot2 = customerPhoneSubquery.from(CustomerProfile.class);
            customerPhoneSubquery.select(customerRoot2.get("customerId"))
                .where(cb.like(cb.lower(customerRoot2.get("phone")), like));
            predicates.add(root.get("customerId").in(customerPhoneSubquery));
            
            // === 5. SEARCH TRONG LICENSE PLATE (SUBQUERY) ===
            Subquery<Integer> vehicleSubquery = query.subquery(Integer.class);
            var vehicleRoot = vehicleSubquery.from(Vehicle.class);
            vehicleSubquery.select(vehicleRoot.get("vehicleId"))
                .where(cb.like(cb.lower(vehicleRoot.get("licensePlate")), like));
            predicates.add(root.get("vehicleId").in(vehicleSubquery));
            
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
