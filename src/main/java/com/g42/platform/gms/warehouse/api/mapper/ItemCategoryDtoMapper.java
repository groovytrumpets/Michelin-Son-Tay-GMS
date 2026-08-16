package com.g42.platform.gms.warehouse.api.mapper;

import com.g42.platform.gms.warehouse.api.dto.ItemCategoryDto;
import com.g42.platform.gms.warehouse.domain.entity.ItemCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemCategoryDtoMapper {
    ItemCategoryDto toDto(ItemCategory itemCategory);
}
