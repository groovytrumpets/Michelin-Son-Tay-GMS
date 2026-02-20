package com.g42.platform.gms.booking_management.infrastructure.mapper;

import com.g42.platform.gms.booking_management.domain.entity.CatalogItem;
import com.g42.platform.gms.booking_management.infrastructure.entity.CatalogItemJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CatalogItemManageMapper {
    CatalogItem toDomain(CatalogItemJpa catalogItemJpa);
}