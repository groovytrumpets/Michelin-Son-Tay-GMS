package com.g42.platform.gms.warehouse.infrastructure.mapper;

import com.g42.platform.gms.warehouse.domain.entity.WarehousePricing;
import com.g42.platform.gms.warehouse.infrastructure.entity.WarehousePricingJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WarehousePricingJpaMapper {
    WarehousePricing toDomain(WarehousePricingJpa warehousePricingJpa );
}
