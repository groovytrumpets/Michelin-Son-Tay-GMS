package com.g42.platform.gms.marketing.service_catalog.infrastructure.mapper;

import com.g42.platform.gms.marketing.service_catalog.domain.entity.Service;
import com.g42.platform.gms.marketing.service_catalog.domain.entity.ServiceMedia;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceJpaEntity;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceMediaJpaEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring",
uses = {ServiceMediaMapper.class}
)
public interface ServiceMapper {
    Service toDomain(ServiceJpaEntity serviceJpaEntity);
    List<Service> toDomain(List<ServiceJpaEntity> serviceJpaEntities);
    ServiceJpaEntity toJpaEntity(Service service);
    List<ServiceJpaEntity> toJpaEntities(List<Service> services);

}
