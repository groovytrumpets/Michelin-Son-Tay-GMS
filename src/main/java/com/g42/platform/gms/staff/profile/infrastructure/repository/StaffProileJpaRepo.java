package com.g42.platform.gms.staff.profile.infrastructure.repository;


import com.g42.platform.gms.service_ticket_management.api.dto.assign.RoleDto;
import com.g42.platform.gms.staff.profile.domain.entity.StaffAuth;
import com.g42.platform.gms.staff.profile.domain.entity.StaffProfile;
import com.g42.platform.gms.staff.profile.infrastructure.entity.StaffProfileJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;


@Repository
public interface StaffProileJpaRepo extends JpaRepository<StaffProfileJpa, Integer> {
    @Query("""
       SELECT DISTINCT sp FROM StaffProfileJpa sp
       LEFT JOIN FETCH sp.staffAuth sa
       LEFT JOIN FETCH sp.roles r
       WHERE (:search IS NULL OR sp.fullName LIKE %:search% OR sp.phone LIKE %:search%)
       AND (:status IS NULL OR sa.status = :status)
       AND (:roleIds IS NULL OR r.id IN :roleIds)
   """)
    Page<StaffProfileJpa> findAllWithFilter(
            @Param("search") String search,
            @Param("status") String status,
            @Param("roleIds") List<Integer> roleIds,
            Pageable pageable
    );


    StaffProfileJpa findByStaffId(Integer staffId);


    boolean existsByPhone(String phone);
    @Query("""
   SELECT DISTINCT sp FROM StaffProfileJpa sp
   JOIN sp.roles r
   WHERE (
       (:role = 'ADVISOR' AND (
           sp.staffId NOT IN (
               SELECT sta.staffId FROM ServiceTicketAssignmentJpa sta
               WHERE (sta.status = 'ACTIVE' OR sta.status = 'PENDING')
               AND sta.roleInTicket = 'TECHNICIAN'
               AND sta.serviceTicketId IN (
                   SELECT st.serviceTicketId FROM ServiceTicketManagement st
                   WHERE st.ticketStatus = 'DRAFT'
                   OR st.ticketStatus = 'IN_PROGRESS'
                   OR st.ticketStatus = 'INSPECTION'
               )
           )
           AND (
               :ticketId = 0 OR sp.staffId NOT IN (
                   SELECT sta2.staffId FROM ServiceTicketAssignmentJpa sta2
                   WHERE sta2.serviceTicketId = :ticketId
                   AND sta2.roleInTicket = 'TECHNICIAN'
                   AND (sta2.status = 'ACTIVE' OR sta2.status = 'PENDING')
               )
           )
       ))
       OR
       (:role != 'ADVISOR' AND sp.staffId NOT IN (
           SELECT sta.staffId FROM ServiceTicketAssignmentJpa sta
           WHERE (sta.status = 'ACTIVE' OR sta.status = 'PENDING')
           AND sta.roleInTicket = 'TECHNICIAN'
           AND sta.serviceTicketId IN (
               SELECT st.serviceTicketId FROM ServiceTicketManagement st
               WHERE st.ticketStatus = 'DRAFT'
               OR st.ticketStatus = 'IN_PROGRESS'
               OR st.ticketStatus = 'INSPECTION'
           )
       )
       AND sp.staffId NOT IN (
           SELECT sta2.staffId FROM ServiceTicketAssignmentJpa sta2
           WHERE sta2.serviceTicketId = :ticketId
           AND sta2.roleInTicket = 'TECHNICIAN'
           AND (sta2.status = 'ACTIVE' OR sta2.status = 'PENDING')
       ))
   )
   AND r.roleCode = :role
""")
    List<StaffProfileJpa> findAvailableStaffByRole(@Param("role") String role, @Param("ticketId") Integer ticketId);


    /** Kiểm tra staff có role cụ thể không (dùng để validate khi assign). */
    @Query("""
   SELECT COUNT(sp) > 0 FROM StaffProfileJpa sp
   JOIN sp.roles r
   WHERE sp.staffId = :staffId
   AND r.roleCode = :role
""")
    boolean existsByStaffIdAndRole(@Param("staffId") Integer staffId, @Param("role") String role);

    /** Lấy tất cả staff có role cụ thể (không lọc bận/rảnh). */
    @Query("""
   SELECT DISTINCT sp FROM StaffProfileJpa sp
   JOIN FETCH sp.roles r
   WHERE r.roleCode = :role
""")
    List<StaffProfileJpa> findAllByRole(@Param("role") String role);
}

