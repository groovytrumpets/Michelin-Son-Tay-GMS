package com.g42.platform.gms.estimation.domain.repository;

import com.g42.platform.gms.estimation.domain.entity.ServiceReminder;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RemindRepo {
    ServiceReminder save(ServiceReminder domain, Integer staffId);

    ServiceReminder findByServiceTicket(Integer serviceTicketId);

    List<ServiceReminder> findByCustomerId(Integer customerId);

    List<ServiceReminder> findByVehicleId(Integer vehicleId);

    List<ServiceReminder> findByCusIdAndVehicle(Integer customerId, Integer vehicleId);

    ServiceReminder updateStatusRemind(Integer remindId, String status, String reason);
}
