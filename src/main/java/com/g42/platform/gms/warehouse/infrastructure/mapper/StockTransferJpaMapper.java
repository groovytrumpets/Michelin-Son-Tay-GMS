package com.g42.platform.gms.warehouse.infrastructure.mapper;

import com.g42.platform.gms.warehouse.domain.entity.StockTransfer;
import com.g42.platform.gms.warehouse.infrastructure.entity.StockTransferJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockTransferJpaMapper {
    StockTransfer toDomain(StockTransferJpa stockTransferJpa );
}
