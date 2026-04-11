package com.g42.platform.gms.estimation.infrastructure.repository;

import com.g42.platform.gms.estimation.api.dto.RemindSearchDto;
import com.g42.platform.gms.estimation.domain.entity.ServiceReminder;
import com.g42.platform.gms.estimation.infrastructure.entity.ServiceReminderJpa;
import org.springframework.beans.PropertyValues;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ServiceRemindJpaRepo extends JpaRepository<ServiceReminderJpa,Integer>, JpaSpecificationExecutor<ServiceReminderJpa> {
    ServiceReminderJpa findByServiceTicketId(Integer serviceTicketId);

    List<ServiceReminderJpa> findAllByCustomerId(Integer customerId);

    List<ServiceReminderJpa> findAllByVehicleId(Integer vehicleId);

    List<ServiceReminderJpa> findAllByCustomerIdAndVehicleId(Integer customerId, Integer vehicleId);

    List<ServiceReminderJpa> findAllByCustomerIdOrderByCreatedAtDesc(Integer customerId);

    List<ServiceReminderJpa> findAllByVehicleIdOrderByCreatedAtDesc(Integer vehicleId);

    List<ServiceReminderJpa> findAllByCustomerIdAndVehicleIdOrderByCreatedAtDesc(Integer customerId, Integer vehicleId);

    List<ServiceReminderJpa> findAllByServiceTicketId(Integer serviceTicketId);
    @Query("""
    select new com.g42.platform.gms.estimation.api.dto.RemindSearchDto(
        r.reminderId,
            c.customerId,
                c.fullName,
                    c.phone,
                        v.vehicleId,
                            v.licensePlate,
                                t.serviceTicketId, 
                                    t.ticketCode,
                                r.reminderDate,
                                    r.reminderTime,
                                        r.note,
                                            r.status,
                                                r.reason,
                                                    s.fullName
        )
            from ServiceReminderJpa r
                left join CustomerProfileJpa c on r.customerId=c.customerId
                    left join Vehicle v on r.vehicleId=v.vehicleId
                        left join ServiceTicketManagement t on r.serviceTicketId=t.serviceTicketId
                            left join StaffProfile s on r.staffId=s.staffId
                            where (:status IS NULL OR r.status = :status)
                                and (:datePart IS NULL OR r.reminderDate = :datePart)
                                    and (:search IS NULL OR 
                                        LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR\s
                                        LOWER(v.licensePlate) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
    Page<RemindSearchDto> searchAllCustom(
            @Param("status") String status,
            @Param("datePart") LocalDate datePart,
            @Param("search") String search,
            Pageable pageable
    );
}
