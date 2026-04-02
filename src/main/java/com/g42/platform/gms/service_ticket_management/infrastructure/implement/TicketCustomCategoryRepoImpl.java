package com.g42.platform.gms.service_ticket_management.infrastructure.implement;

import com.g42.platform.gms.service_ticket_management.domain.entity.TicketCustomCategory;
import com.g42.platform.gms.service_ticket_management.domain.repository.TicketCustomCategoryRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.SafetyInspectionInfraMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.TicketCustomCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TicketCustomCategoryRepoImpl implements TicketCustomCategoryRepo {

    private final TicketCustomCategoryRepository jpaRepo;
    private final SafetyInspectionInfraMapper mapper;

    @Override
    public boolean existsByInspectionIdAndCategoryName(Integer inspectionId, String categoryName) {
        return jpaRepo.existsByInspectionIdAndCategoryName(inspectionId, categoryName);
    }

    @Override
    public TicketCustomCategory save(TicketCustomCategory customCategory) {
        return mapper.toDomain(jpaRepo.save(mapper.toJpa(customCategory)));
    }

    @Override
    public Optional<TicketCustomCategory> findById(Integer id) {
        return jpaRepo.findById(id).map(mapper::toDomain);
    }
}
