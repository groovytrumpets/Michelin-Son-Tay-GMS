package com.g42.platform.gms.warehouse.infrastructure.mapper;

import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryJpaMapper {
    Inventory toDomain(InventoryJpa inventoryJpa );
}
