package com.g42.platform.gms.marketing.service_combo.infrastructure.mapper;

import com.g42.platform.gms.marketing.service_combo.api.dto.ComboResDto;
import com.g42.platform.gms.marketing.service_combo.domain.entity.ComboItem;
import com.g42.platform.gms.marketing.service_combo.infrastructure.entity.ComboItemJpa;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ComboItemJpaMapper {

    ComboItemJpa toJpa(ComboItem comboItem);

    ComboItem toDomain(ComboItemJpa comboItemJpa);

    ComboItem fromRestDto(ComboResDto updateItem);
}
