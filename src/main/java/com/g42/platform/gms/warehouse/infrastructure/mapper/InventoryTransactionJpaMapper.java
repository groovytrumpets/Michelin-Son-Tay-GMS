package com.g42.platform.gms.warehouse.infrastructure.mapper;

import com.g42.platform.gms.warehouse.domain.entity.Inventory;
import com.g42.platform.gms.warehouse.domain.entity.InventoryTransaction;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.InventoryTransactionJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryTransactionJpaMapper {
    InventoryTransaction toDomain(InventoryTransactionJpa inventoryTransJpa );
}
