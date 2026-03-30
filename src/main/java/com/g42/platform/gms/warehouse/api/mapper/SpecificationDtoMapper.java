package com.g42.platform.gms.warehouse.api.mapper;

import com.g42.platform.gms.warehouse.api.dto.BrandHintDto;
import com.g42.platform.gms.warehouse.api.dto.SpecificationDto;
import com.g42.platform.gms.warehouse.domain.entity.Brand;
import com.g42.platform.gms.warehouse.domain.entity.Specification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SpecificationDtoMapper {
    SpecificationDto toDto(Specification specification);
}
