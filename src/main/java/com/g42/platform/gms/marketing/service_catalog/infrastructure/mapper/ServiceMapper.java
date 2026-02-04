package com.g42.platform.gms.marketing.service_catalog.infrastructure.mapper;

import com.g42.platform.gms.marketing.service_catalog.domain.entity.Service;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring",
uses = {ServiceMediaMapper.class}
)
public interface ServiceMapper {
    Service toDomain(ServiceJpaEntity serviceJpaEntity);
    ServiceJpaEntity toJpaEntity(Service service);
}
