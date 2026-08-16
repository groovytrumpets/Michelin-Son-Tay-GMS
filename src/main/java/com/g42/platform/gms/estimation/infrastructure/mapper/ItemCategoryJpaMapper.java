package com.g42.platform.gms.estimation.infrastructure.mapper;

import com.g42.platform.gms.estimation.domain.entity.ItemCategory;
import com.g42.platform.gms.estimation.infrastructure.entity.ItemCategoryJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemCategoryJpaMapper {

    ItemCategory toDomain(ItemCategoryJpaEntity itemCategoryJpaEntity);

    ItemCategoryJpaEntity toJpa(ItemCategory itemCategory);
}
