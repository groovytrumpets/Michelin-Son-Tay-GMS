package com.g42.platform.gms.service_ticket_management.infrastructure.implement;

import com.g42.platform.gms.service_ticket_management.domain.entity.OdometerReading;
import com.g42.platform.gms.service_ticket_management.domain.repository.OdometerReadingRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.OdometerReadingMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.OdometerHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OdometerReadingRepoImpl implements OdometerReadingRepo {

    private final OdometerHistoryRepository jpaRepo;
    private final OdometerReadingMapper mapper;

    @Override
    public Optional<OdometerReading> findLatestByVehicleId(Integer vehicleId) {
        return jpaRepo.findLatestByVehicleId(vehicleId).map(mapper::toDomain);
    }

    @Override
    public List<OdometerReading> findByServiceTicketId(Integer serviceTicketId) {
        return jpaRepo.findByServiceTicketId(serviceTicketId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public OdometerReading save(OdometerReading reading) {
        return mapper.toDomain(jpaRepo.save(mapper.toJpa(reading)));
    }
}
