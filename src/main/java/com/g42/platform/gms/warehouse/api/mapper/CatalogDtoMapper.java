package com.g42.platform.gms.warehouse.api.mapper;

import com.g42.platform.gms.warehouse.api.dto.*;
import com.g42.platform.gms.warehouse.domain.entity.CatalogItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CatalogDtoMapper {
    CatalogItemDto toDto(CatalogItem catalogItem);
    CatalogItem toDomain(CatalogCreateDto catalogCreateDto);

    CatalogSummaryDto toSumaryDto(CatalogItem catalogItem);

    CatalogDetailDto toDetailDto(CatalogItem catalogItem);

    CatalogWarehouseDto toSumaryWarehouseDto(CatalogItem catalogItem);
}
