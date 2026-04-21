package com.g42.platform.gms.marketing.service_combo.api.mapper;

import com.g42.platform.gms.marketing.service_combo.api.dto.ComboCreateDto;
import com.g42.platform.gms.marketing.service_combo.api.dto.ComboResDto;
import com.g42.platform.gms.marketing.service_combo.domain.entity.ComboItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ComboItemDtoMapper {

    ComboResDto toDto(ComboItem comboItem);

    ComboItem toDomain(ComboCreateDto comboCreateDto);

    ComboCreateDto toCreateDto(ComboItem comboItem);
}
