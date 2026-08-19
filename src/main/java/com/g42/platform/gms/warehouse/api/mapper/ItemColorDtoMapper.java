package com.g42.platform.gms.warehouse.api.mapper;

import com.g42.platform.gms.warehouse.api.dto.ItemColorDto;
import com.g42.platform.gms.warehouse.domain.entity.ItemColor;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemColorDtoMapper {
    ItemColorDto toDto(ItemColor itemColor);

    ItemColor toDomain(ItemColorDto itemColorDto);
}
