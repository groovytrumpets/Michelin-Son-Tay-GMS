package com.g42.platform.gms.marketing.service_catalog.infrastructure.mapper;

import com.g42.platform.gms.marketing.service_catalog.domain.entity.ServiceMedia;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.entity.ServiceMediaJpaEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ServiceMediaMapper {
    ServiceMedia toDomain(ServiceMediaJpaEntity serviceMediaJpaEntity);
    ServiceMediaJpaEntity toJpa(ServiceMedia serviceMedia);
    List<ServiceMediaJpaEntity> toJpaEntities(List<ServiceMedia> serviceMedia);
    List<ServiceMedia>  toJpaModel(List<ServiceMediaJpaEntity> serviceMediaJpaEntities);
}
