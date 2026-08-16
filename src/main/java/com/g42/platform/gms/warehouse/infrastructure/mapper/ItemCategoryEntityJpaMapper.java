package com.g42.platform.gms.warehouse.infrastructure.mapper;

import com.g42.platform.gms.warehouse.domain.entity.ItemCategory;
import com.g42.platform.gms.warehouse.infrastructure.entity.ItemCategoryJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemCategoryEntityJpaMapper {

    ItemCategory toDomain(ItemCategoryJpa itemCategoryJpa);

    ItemCategoryJpa toJpa(ItemCategory itemCategory);
}
