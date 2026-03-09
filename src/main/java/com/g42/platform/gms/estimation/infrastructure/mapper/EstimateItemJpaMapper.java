package com.g42.platform.gms.estimation.infrastructure.mapper;

import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateItemJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EstimateItemJpaMapper {
    EstimateItem toDomain(EstimateItemJpa estimateItemJpa);
}
