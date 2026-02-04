package com.g42.platform.gms.marketing.service_catalog.infrastructure;

import com.g42.platform.gms.marketing.service_catalog.domain.repository.ServiceRepository;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.mapper.ServiceMapper;
import com.g42.platform.gms.marketing.service_catalog.infrastructure.repository.ServiceJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class ServiceRepositoryImpl implements ServiceRepository {
    private final ServiceJpaRepository serviceJpaRepository;
    private final ServiceMapper serviceMapper;

    public ServiceRepositoryImpl(ServiceJpaRepository serviceJpaRepository, ServiceMapper serviceMapper) {
        this.serviceJpaRepository = serviceJpaRepository;
        this.serviceMapper = serviceMapper;
    }
}
