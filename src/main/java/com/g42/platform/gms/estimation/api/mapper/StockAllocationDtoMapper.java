package com.g42.platform.gms.estimation.api.mapper;

import com.g42.platform.gms.estimation.api.dto.StockAllocationDto;
import com.g42.platform.gms.estimation.domain.entity.StockAllocation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockAllocationDtoMapper {
    StockAllocationDto toDto(StockAllocation stockAllocation);
}
