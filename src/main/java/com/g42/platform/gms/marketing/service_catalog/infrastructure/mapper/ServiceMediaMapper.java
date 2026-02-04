package com.g42.platform.gms.marketing.service_catalog.infrastructure.mapper;

import com.g42.platform.gms.marketing.service_catalog.domain.entity.ServiceMedia;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceMediaJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ServiceMediaMapper {
    ServiceMedia toDomain(ServiceMediaJpaEntity serviceMediaJpaEntity);
    ServiceMediaJpaEntity toJpa(ServiceMedia serviceMedia);
}
