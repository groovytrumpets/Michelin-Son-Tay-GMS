package com.g42.platform.gms.estimation.infrastructure.mapper;

import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EstimateJpaMapper {
    Estimate toDomain(EstimateJpa estimateJpa);
}
