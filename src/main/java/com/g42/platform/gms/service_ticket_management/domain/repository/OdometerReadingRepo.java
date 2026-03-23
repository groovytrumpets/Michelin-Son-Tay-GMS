package com.g42.platform.gms.service_ticket_management.domain.repository;

import com.g42.platform.gms.service_ticket_management.domain.entity.OdometerReading;

import java.util.List;
import java.util.Optional;

public interface OdometerReadingRepo {

    Optional<OdometerReading> findLatestByVehicleId(Integer vehicleId);

    List<OdometerReading> findByServiceTicketId(Integer serviceTicketId);

    OdometerReading save(OdometerReading reading);
}
