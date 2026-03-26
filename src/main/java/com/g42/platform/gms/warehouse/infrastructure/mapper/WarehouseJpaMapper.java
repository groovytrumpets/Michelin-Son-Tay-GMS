package com.g42.platform.gms.warehouse.infrastructure.mapper;

import com.g42.platform.gms.warehouse.domain.entity.Warehouse;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehouseJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WarehouseJpaMapper {
    Warehouse toDomain(WarehouseJpa warehouseJpa );
}
