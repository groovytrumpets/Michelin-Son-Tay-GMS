package com.g42.platform.gms.service_ticket_management.infrastructure.implement;

import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspection;
import com.g42.platform.gms.service_ticket_management.domain.repository.SafetyInspectionRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyInspectionJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.SafetyInspectionInfraMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.SafetyInspectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SafetyInspectionRepoImpl implements SafetyInspectionRepo {

    private final SafetyInspectionRepository jpaRepo;
    private final SafetyInspectionInfraMapper mapper;

    @Override
    public Optional<SafetyInspection> findByServiceTicketId(Integer serviceTicketId) {
        return jpaRepo.findByServiceTicketId(serviceTicketId).map(mapper::toDomain);
    }

    @Override
    public Optional<SafetyInspection> findById(Integer inspectionId) {
        return jpaRepo.findById(inspectionId).map(mapper::toDomain);
    }

    @Override
    public SafetyInspection save(SafetyInspection inspection) {
        SafetyInspectionJpa jpa = mapper.toJpa(inspection);
        return mapper.toDomain(jpaRepo.save(jpa));
    }

    @Override
    public SafetyInspection findByIdService(Integer serviceTicketId) {
        SafetyInspectionJpa safetyInspectionJpa = jpaRepo.findByServiceTicketId2(serviceTicketId);
        return mapper.toDomain(safetyInspectionJpa);
    }
}
