package com.g42.platform.gms.warehouse.infrastructure.mapper;

import com.g42.platform.gms.warehouse.domain.entity.Brand;
import com.g42.platform.gms.warehouse.domain.entity.SpecAttribute;
import com.g42.platform.gms.warehouse.infrastructure.entity.BrandJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.SpecAttributeJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SpecAttributeJpaMapper {
    SpecAttribute toDomain(SpecAttributeJpa specAttributeJpa);

    SpecAttributeJpa toJpa(SpecAttribute specAttribute);
}
