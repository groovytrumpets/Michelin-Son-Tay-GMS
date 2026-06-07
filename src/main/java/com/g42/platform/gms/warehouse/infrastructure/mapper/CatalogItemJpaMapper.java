package com.g42.platform.gms.warehouse.infrastructure.mapper;

import com.g42.platform.gms.warehouse.api.dto.CatalogItemDto;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import com.g42.platform.gms.warehouse.infrastructure.entity.CatalogItemJpa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CatalogItemJpaMapper {
    @Mapping(source = "serviceId", target = "serviceServiceId")
    CatalogItem toDomain(CatalogItemJpa catalogItemJpa );

    @Mapping(source = "serviceServiceId", target = "serviceId")
    CatalogItemJpa toJpa(CatalogItem catalogItem);

    @Mapping(source = "serviceId", target = "serviceServiceId")
    CatalogItemDto toDto(CatalogItemJpa catalogItemJpa);
}
