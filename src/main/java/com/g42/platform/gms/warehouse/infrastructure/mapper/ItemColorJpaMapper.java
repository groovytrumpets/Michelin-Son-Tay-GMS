package com.g42.platform.gms.warehouse.infrastructure.mapper;

import com.g42.platform.gms.warehouse.domain.entity.ItemColor;
import com.g42.platform.gms.warehouse.infrastructure.entity.ItemColorJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemColorJpaMapper {
    ItemColor toDomain(ItemColorJpa itemColorJpa);

    ItemColorJpa toJpa(ItemColor itemColor);
}
