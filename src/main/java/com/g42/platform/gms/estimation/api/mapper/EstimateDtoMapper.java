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
    @Mapping(target = "estimateItemId", source = "id")
    @Mapping(target = "taxRuleId", source = "taxRuleId")
    @Mapping(target = "taxCode", ignore = true)   // set thủ công trong service
    @Mapping(target = "taxRate", ignore = true)
    EstimateItemDto toEstimateItemDto(EstimateItem estimateItem);
    @Mapping(target = "subTotal", expression = "java(estimateItem.getSubTotal())")
    List<EstimateItemDto> toEstimateItemDto(List<EstimateItem> estimateItems);
    @Mapping(target = "estimateId", source = "id")
    EstimateRespondDto toEstimateDto(Estimate estimate);
    @Mapping(target = "workCateId", source = "id")
    WorkCataDto toWorkCateDto(WorkCategory workCataDto);
}
