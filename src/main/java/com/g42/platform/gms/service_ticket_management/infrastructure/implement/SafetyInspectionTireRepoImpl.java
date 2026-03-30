package com.g42.platform.gms.service_ticket_management.infrastructure.implement;

import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspectionTire;
import com.g42.platform.gms.service_ticket_management.domain.repository.SafetyInspectionTireRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyInspectionTireJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.SafetyInspectionInfraMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.SafetyInspectionTireRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SafetyInspectionTireRepoImpl implements SafetyInspectionTireRepo {

    private final SafetyInspectionTireRepository jpaRepo;
    private final SafetyInspectionInfraMapper mapper;

    @Override
    public List<SafetyInspectionTire> findByInspectionId(Integer inspectionId) {
        return mapper.tiresToDomain(jpaRepo.findByInspectionId(inspectionId));
    }

    @Override
    public SafetyInspectionTire save(SafetyInspectionTire tire) {
        SafetyInspectionTireJpa jpa = mapper.toJpa(tire);
        return mapper.toDomain(jpaRepo.save(jpa));
    }

    @Override
    public void deleteAll(List<SafetyInspectionTire> tires) {
        jpaRepo.deleteAll(mapper.tiresToJpa(tires));
    }

    @Override
    public void delete(SafetyInspectionTire tire) {
        jpaRepo.delete(mapper.toJpa(tire));
    }
}
