package com.g42.platform.gms.warehouse.api.mapper;

import com.g42.platform.gms.warehouse.api.dto.BrandHintDto;
import com.g42.platform.gms.warehouse.domain.entity.Brand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BrandDtoMapper {
    BrandHintDto toDto(Brand brand);
}
