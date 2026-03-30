package com.g42.platform.gms.warehouse.api.mapper;

import com.g42.platform.gms.warehouse.api.dto.BrandHintDto;
import com.g42.platform.gms.warehouse.api.dto.ProductLineDto;
import com.g42.platform.gms.warehouse.domain.entity.Brand;
import com.g42.platform.gms.warehouse.domain.entity.ProductLine;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductLineDtoMapper {
    ProductLineDto toDto(ProductLine productLine);
}
