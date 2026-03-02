package com.g42.platform.gms.booking_management.infrastructure.mapper;

import com.g42.platform.gms.booking_management.domain.entity.CatalogItem;
import com.g42.platform.gms.booking_management.infrastructure.entity.CatalogItemJpa;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CatalogItemManageMapper {
    CatalogItem toDomain(CatalogItemJpa catalogItemJpa);

    List<CatalogItem> getListOfCatalogItem(List<CatalogItemJpa> catalogItems);
}