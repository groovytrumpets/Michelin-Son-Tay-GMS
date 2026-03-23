package com.g42.platform.gms.service_ticket_management.infrastructure.implement;

import com.g42.platform.gms.service_ticket_management.domain.entity.WorkCategory;
import com.g42.platform.gms.service_ticket_management.domain.repository.WorkCategoryRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.WorkCategoryInfraMapper;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.WorkCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class WorkCategoryRepoImpl implements WorkCategoryRepo {

    private final WorkCategoryRepository workCategoryRepository;
    private final WorkCategoryInfraMapper workCategoryInfraMapper;

    @Override
    public List<String> findDefaultWorkCategoryNames() {
        return workCategoryRepository.findDefaultWorkCategoryNames();
    }

    @Override
    public List<WorkCategory> findDefaultCategories() {
        return workCategoryInfraMapper.toDomainList(
            workCategoryRepository.findActiveCategories().stream()
                .filter(c -> c.getIsDefault() != null && c.getIsDefault())
                .toList()
        );
    }

    @Override
    public List<WorkCategory> findActiveCategories() {
        return workCategoryInfraMapper.toDomainList(workCategoryRepository.findActiveCategories());
    }
}
