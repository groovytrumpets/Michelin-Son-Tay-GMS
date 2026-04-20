package com.g42.platform.gms.estimation.domain.repository;

import com.g42.platform.gms.estimation.api.dto.RemindSearchDto;
import com.g42.platform.gms.estimation.domain.entity.ServiceReminder;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RemindRepo {
    ServiceReminder save(ServiceReminder domain, Integer staffId);

    List<ServiceReminder> findByServiceTicket(Integer serviceTicketId);

    List<ServiceReminder> findByCustomerId(Integer customerId);

    List<ServiceReminder> findByVehicleId(Integer vehicleId);

    List<ServiceReminder> findByCusIdAndVehicle(Integer customerId, Integer vehicleId);

    ServiceReminder updateStatusRemind(Integer remindId, String status, String reason);

    Page<RemindSearchDto> searchReminders(int page, int size, LocalDateTime date, String status, String search, String sortBy);
}
