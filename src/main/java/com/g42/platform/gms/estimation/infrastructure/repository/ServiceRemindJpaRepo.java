package com.g42.platform.gms.estimation.infrastructure.repository;

import com.g42.platform.gms.estimation.domain.entity.ServiceReminder;
import com.g42.platform.gms.estimation.infrastructure.entity.ServiceReminderJpa;
import org.springframework.beans.PropertyValues;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

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
}
