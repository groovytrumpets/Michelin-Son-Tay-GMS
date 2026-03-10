package com.g42.platform.gms.estimation.api.mapper;

import com.g42.platform.gms.estimation.api.dto.EstimateItemDto;
import com.g42.platform.gms.estimation.api.dto.EstimateRespondDto;
import com.g42.platform.gms.estimation.api.dto.WorkCataDto;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.entity.WorkCategory;
import com.g42.platform.gms.estimation.infrastructure.entity.WorkCategoryJpa;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EstimateDtoMapper {
    EstimateItemDto toEstimateItemDto(EstimateItem estimateItem);
    List<EstimateItemDto> toEstimateItemDto(List<EstimateItem> estimateItems);
    EstimateRespondDto toEstimateDto(Estimate estimate);
    WorkCataDto toDto(WorkCategory workCataDto);
}
