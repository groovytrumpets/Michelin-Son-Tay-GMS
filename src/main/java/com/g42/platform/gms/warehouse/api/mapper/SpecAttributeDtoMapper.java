package com.g42.platform.gms.warehouse.api.mapper;

import com.g42.platform.gms.warehouse.api.dto.SpecAttributeDto;
import com.g42.platform.gms.warehouse.api.dto.SpecificationDto;
import com.g42.platform.gms.warehouse.domain.entity.SpecAttribute;
import com.g42.platform.gms.warehouse.domain.entity.Specification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SpecAttributeDtoMapper {
    SpecAttributeDto toDto(SpecAttribute specAttribute);
}
