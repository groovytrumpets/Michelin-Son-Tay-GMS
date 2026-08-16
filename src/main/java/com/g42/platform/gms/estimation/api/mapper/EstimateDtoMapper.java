package com.g42.platform.gms.estimation.api.mapper;

import com.g42.platform.gms.estimation.api.dto.EstimateItemDto;
import com.g42.platform.gms.estimation.api.dto.EstimateRespondDto;
import com.g42.platform.gms.estimation.api.dto.ItemCateDto;
import com.g42.platform.gms.estimation.api.dto.request.EstimateItemReqDto;
import com.g42.platform.gms.estimation.domain.entity.Estimate;
import com.g42.platform.gms.estimation.domain.entity.EstimateItem;
import com.g42.platform.gms.estimation.domain.entity.ItemCategory;
import com.g42.platform.gms.estimation.infrastructure.entity.EstimateItemJpa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EstimateDtoMapper {
    @Mapping(target = "estimateItemId", source = "id")
    @Mapping(target = "itemCategory", ignore = true)
    @Mapping(target = "subTotal", source = "totalPrice")
    EstimateItemDto toEstimateItemDto(EstimateItem estimateItem);
    @Mapping(target = "subTotal", source = "totalPrice")
    List<EstimateItemDto> toEstimateItemDto(List<EstimateItem> estimateItems);
    @Mapping(target = "estimateId", source = "id")
    EstimateRespondDto toEstimateDto(Estimate estimate);
    @Mapping(target = "itemCategoryId", source = "id")
    ItemCateDto toItemCateDto(ItemCategory itemCategory);
    @Mapping(target = "estimateItemId", source = "id")
    EstimateItemReqDto toEstimateItemReqDto(EstimateItem estimateItem);
    @Mapping(target = "estimateItemId", source = "id")
    EstimateItemDto toEstimateItemDtoJpa(EstimateItemJpa estimateItem);
}
