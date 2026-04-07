package com.g42.platform.gms.warehouse.infrastructure.mapper;

import com.g42.platform.gms.warehouse.domain.entity.Brand;
import com.g42.platform.gms.warehouse.domain.entity.WorkCategory;
import com.g42.platform.gms.warehouse.infrastructure.entity.BrandJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ItemCategoryJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.WorkCategoryJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkCategoryEntityJpaMapper {
    WorkCategory toDomain(WorkCategoryJpaEntity workCategoryJpaEntity);

    WorkCategoryJpaEntity toJpa(WorkCategory workCategory);
}
