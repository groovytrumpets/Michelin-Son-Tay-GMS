package com.g42.platform.gms.service_ticket_management.infrastructure.implement;

import com.g42.platform.gms.service_ticket_management.domain.entity.SafetyInspectionItem;
import com.g42.platform.gms.service_ticket_management.domain.repository.SafetyInspectionItemRepo;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyInspectionItemJpa;
import com.g42.platform.gms.service_ticket_management.infrastructure.mapper.SafetyInspectionInfraMapper;
import com.g42.platform.gms.service_ticket_management.domain.projection.SafetyInspectionItemWithCategory;
import com.g42.platform.gms.service_ticket_management.infrastructure.repository.SafetyInspectionItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SafetyInspectionItemRepoImpl implements SafetyInspectionItemRepo {

    private final SafetyInspectionItemRepository jpaRepo;
    private final SafetyInspectionInfraMapper mapper;

    @Override
    public List<SafetyInspectionItem> findByInspectionId(Integer inspectionId) {
        return mapper.itemsToDomain(jpaRepo.findByInspectionId(inspectionId));
    }

    @Override
    public Optional<SafetyInspectionItem> findByInspectionIdAndWorkCategoryId(Integer inspectionId, Integer workCategoryId) {
        return jpaRepo.findByInspectionIdAndWorkCategoryId(inspectionId, workCategoryId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<SafetyInspectionItem> findByInspectionIdAndCustomCategoryId(Integer inspectionId, Integer customCategoryId) {
        return jpaRepo.findByInspectionIdAndCustomCategoryId(inspectionId, customCategoryId)
                .map(mapper::toDomain);
    }

    @Override
    public List<SafetyInspectionItemWithCategory> findByInspectionIdWithCategory(Integer inspectionId) {
        return jpaRepo.findByInspectionIdWithCategory(inspectionId);
    }

    @Override
    public List<SafetyInspectionItem> findItemsWithCategory(Integer inspectionId) {
        return mapper.projectionsToItems(jpaRepo.findByInspectionIdWithCategory(inspectionId));
    }

    @Override
    public SafetyInspectionItem save(SafetyInspectionItem item) {
        SafetyInspectionItemJpa jpa = mapper.toJpa(item);
        return mapper.toDomain(jpaRepo.save(jpa));
    }

    @Override
    public void deleteAll(List<SafetyInspectionItem> items) {
        jpaRepo.deleteAll(mapper.itemsToJpa(items));
    }

    @Override
    public void delete(SafetyInspectionItem item) {
        jpaRepo.delete(mapper.toJpa(item));
    }
}
