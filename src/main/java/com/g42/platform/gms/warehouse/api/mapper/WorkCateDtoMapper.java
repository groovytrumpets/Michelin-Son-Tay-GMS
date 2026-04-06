package com.g42.platform.gms.warehouse.api.mapper;

import com.g42.platform.gms.warehouse.api.dto.WorkCategoryHintDto;
import com.g42.platform.gms.warehouse.domain.entity.WorkCategory;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WorkCateDtoMapper {
    WorkCategoryHintDto toDto(WorkCategory brand);
}
