package com.g42.platform.gms.warehouse.infrastructure.mapper;

import com.g42.platform.gms.warehouse.domain.entity.Brand;
import com.g42.platform.gms.warehouse.domain.entity.ProductLine;
import com.g42.platform.gms.warehouse.infrastructure.entity.BrandJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ProductLineJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductLineJpaMapper {
    ProductLine toDomain(ProductLineJpa productLineJpa);
}
