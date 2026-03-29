package com.g42.platform.gms.warehouse.infrastructure.mapper;

import com.g42.platform.gms.warehouse.api.dto.ItemCategoryReqDto;
import com.g42.platform.gms.warehouse.domain.entity.Brand;
import com.g42.platform.gms.warehouse.domain.entity.ItemCategory;
import com.g42.platform.gms.warehouse.infrastructure.entity.BrandJpa;
import com.g42.platform.gms.warehouse.infrastructure.entity.ItemCategoryJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemCategoryJpaMapper {
    ItemCategory toDomain(ItemCategoryJpa itemCategoryJpa);

    ItemCategoryJpa toJpa(ItemCategory itemCategory);
    ItemCategoryJpa toDto(ItemCategoryReqDto itemCategoryReqDto);
}
