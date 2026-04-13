package com.g42.platform.gms.warehouse.api.mapper;

import com.g42.platform.gms.warehouse.api.dto.BrandHintDto;
import com.g42.platform.gms.warehouse.api.dto.WarehouseDto;
import com.g42.platform.gms.warehouse.domain.entity.Brand;
import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WarehouseDtoMapper {
    WarehouseDto toDto(Warehouse warehouse);
}
