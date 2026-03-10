package com.g42.platform.gms.estimation.api.mapper;

import com.g42.platform.gms.estimation.api.dto.EstimateItemDto;
import com.g42.platform.gms.estimation.api.dto.EstimateRespondDto;
import com.g42.platform.gms.estimation.api.dto.WorkCataDto;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.entity.WorkCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EstimateDtoMapper {
    @Mapping(target = "subTotal", expression = "java(estimateItem.getSubTotal())")
    EstimateItemDto toEstimateItemDto(EstimateItem estimateItem);
    @Mapping(target = "subTotal", expression = "java(estimateItem.getSubTotal())")
    List<EstimateItemDto> toEstimateItemDto(List<EstimateItem> estimateItems);
    EstimateRespondDto toEstimateDto(Estimate estimate);
    WorkCataDto toWorkCateDto(WorkCategory workCataDto);
}
