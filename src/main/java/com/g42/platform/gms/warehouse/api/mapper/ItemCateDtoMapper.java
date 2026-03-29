package com.g42.platform.gms.warehouse.api.mapper;

import com.g42.platform.gms.warehouse.api.dto.BrandHintDto;
import com.g42.platform.gms.warehouse.api.dto.ItemCategoryHintDto;
import com.g42.platform.gms.warehouse.api.dto.ItemCategoryReqDto;
import com.g42.platform.gms.warehouse.domain.entity.Brand;
import com.g42.platform.gms.warehouse.domain.entity.ItemCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemCateDtoMapper {
    ItemCategoryHintDto toDto(ItemCategory itemCategory);
    ItemCategory toDomain(ItemCategoryReqDto itemCategoryReqDto);
}
