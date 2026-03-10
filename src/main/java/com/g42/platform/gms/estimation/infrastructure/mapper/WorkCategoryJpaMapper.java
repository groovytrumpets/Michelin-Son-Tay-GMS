package com.g42.platform.gms.estimation.infrastructure.mapper;

import com.g42.platform.gms.estimation.domain.entity.WorkCategory;
import com.g42.platform.gms.estimation.infrastructure.entity.WorkCategoryJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkCategoryJpaMapper {
    WorkCategory toDomain(WorkCategoryJpa workCategoryJpa);
}
