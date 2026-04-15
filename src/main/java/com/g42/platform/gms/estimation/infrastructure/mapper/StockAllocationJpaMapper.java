package com.g42.platform.gms.estimation.infrastructure.mapper;

import com.g42.platform.gms.estimation.api.dto.StockAllocationDto;
import com.g42.platform.gms.estimation.domain.entity.StockAllocation;
import com.g42.platform.gms.estimation.infrastructure.entity.StockAllocationJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockAllocationJpaMapper {
    StockAllocationJpa fromDomain(StockAllocation stockAllocation);

    StockAllocation toDomain(StockAllocationJpa stockAllocationJpa);

    StockAllocationDto toDto(StockAllocationJpa stockAllocationJpa);
}
