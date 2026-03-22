package com.g42.platform.gms.service_ticket_management.infrastructure.mapper;

import com.g42.platform.gms.service_ticket_management.domain.entity.WorkCategory;
import com.g42.platform.gms.service_ticket_management.infrastructure.entity.SafetyWorkCategoryJpa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WorkCategoryInfraMapper {
    
    WorkCategory toDomain(SafetyWorkCategoryJpa jpa);
    
    SafetyWorkCategoryJpa toJpa(WorkCategory domain);
    
    List<WorkCategory> toDomainList(List<SafetyWorkCategoryJpa> jpaList);
    
    List<SafetyWorkCategoryJpa> toJpaList(List<WorkCategory> domainList);
}