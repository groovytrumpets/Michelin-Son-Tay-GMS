package com.g42.platform.gms.warehouse.infrastructure.mapper;

import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CatalogItemJpaMapper {
    CatalogItem toDomain(CatalogItemJpa catalogItemJpa );
}
