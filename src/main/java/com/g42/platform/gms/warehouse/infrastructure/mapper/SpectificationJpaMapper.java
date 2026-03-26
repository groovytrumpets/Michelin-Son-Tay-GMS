package com.g42.platform.gms.warehouse.infrastructure.mapper;

import com.g42.platform.gms.warehouse.domain.entity.Brand;
import com.g42.platform.gms.warehouse.domain.entity.Specification;
import com.g42.platform.gms.warehouse.infrastructure.entity.BrandJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.SpecificationJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SpectificationJpaMapper {
    Specification toDomain(SpecificationJpa specificationJpa);
}
